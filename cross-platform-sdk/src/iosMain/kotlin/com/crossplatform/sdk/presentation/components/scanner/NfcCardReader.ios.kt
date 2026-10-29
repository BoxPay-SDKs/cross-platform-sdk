@file:OptIn(ExperimentalForeignApi::class)

package com.crossplatform.sdk.presentation.components.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreNFC.NFCReaderSessionInvalidationErrorUserCanceled
import platform.CoreNFC.NFCReaderSessionInvalidationErrorSessionTimeout
import platform.CoreNFC.NFCISO7816APDU
import platform.CoreNFC.NFCISO7816TagProtocol
import platform.CoreNFC.NFCTagProtocol
import platform.CoreNFC.NFCTagReaderSession
import platform.CoreNFC.NFCTagReaderSessionDelegateProtocol
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.posix.QOS_CLASS_USER_INITIATED
import platform.posix.memcpy

// ─────────────────────────────────────────────────────────────────────────────
// NFC availability
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal actual fun rememberNfcAvailability(): NfcAvailability = remember {
    // readingAvailable returns true when NFC hardware is present and enabled.
    // CoreNFC does not distinguish "no hardware" from "restricted"; both are unavailable.
    if (NFCTagReaderSession.readingAvailable()) NfcAvailability.Available
    else NfcAvailability.NotSupported
}

// ─────────────────────────────────────────────────────────────────────────────
// NFC reader effect
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Starts an [NFCTagReaderSession] while composed.
 * The system presents its own "Hold near reader" sheet.
 * EMV APDU commands run on a background dispatch queue; callbacks fire on the main queue.
 *
 * Host app requirements:
 *   • `NFCReaderUsageDescription` in Info.plist
 *   • Entitlement `com.apple.developer.nfc.readersession.formats` = [ "TAG" ]
 */
@Composable
internal actual fun NfcReaderEffect(
    onScanned: (ScannedCard) -> Unit,
    onError:   (NfcScanError) -> Unit
) {
    DisposableEffect(Unit) {
        val delegate = NfcSessionDelegate(
            onScanned = { card -> dispatch_async(dispatch_get_main_queue()) { onScanned(card) } },
            onError   = { err  -> dispatch_async(dispatch_get_main_queue()) { onError(err)   } }
        )

        // pollingOption 0x03 = NFC_TAG_TYPE_ISO_DEP (ISO 14443-A + B — covers all payment cards)
        val session = NFCTagReaderSession(
            pollingOption = 0x03L,
            delegate      = delegate,
            queue         = null     // nil → main queue for delegate callbacks
        ) ?: run {
            onError(NfcScanError.NotSupported)
            return@DisposableEffect onDispose {}
        }

        delegate.session = session
        session.alertMessage = "Hold your card to the top of your iPhone."
        session.beginSession()

        onDispose {
            runCatching { session.invalidateSession() }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Session delegate
// ─────────────────────────────────────────────────────────────────────────────

private class NfcSessionDelegate(
    private val onScanned: (ScannedCard) -> Unit,
    private val onError:   (NfcScanError) -> Unit
) : NSObject(), NFCTagReaderSessionDelegateProtocol {

    var session: NFCTagReaderSession? = null

    override fun tagReaderSessionDidBecomeActive(session: NFCTagReaderSession) { /* ready */ }

    override fun tagReaderSession(
        session: NFCTagReaderSession,
        didInvalidateWithError: platform.Foundation.NSError
    ) {
        val code = didInvalidateWithError.code
        if (code == NFCReaderSessionInvalidationErrorUserCanceled.toLong() ||
            code == NFCReaderSessionInvalidationErrorSessionTimeout.toLong()
        ) {
            onError(NfcScanError.Cancelled)
        } else {
            onError(NfcScanError.TagLost)
        }
    }

    override fun tagReaderSession(
        session: NFCTagReaderSession,
        didDetectTags: List<*>
    ) {
        val tag = didDetectTags.firstOrNull() as? NFCTagProtocol ?: return

        session.connectToTag(tag) { error ->
            if (error != null) {
                session.invalidateSessionWithErrorMessage("Connection failed.")
                onError(NfcScanError.TagLost)
                return@connectToTag
            }

            val iso7816Tag = tag.asNFCISO7816Tag()
            if (iso7816Tag == null) {
                session.invalidateSessionWithErrorMessage("Card type not supported.")
                onError(NfcScanError.ParseFailed)
                return@connectToTag
            }

            dispatch_async(
                dispatch_get_global_queue(QOS_CLASS_USER_INITIATED.toLong(), 0UL)
            ) {
                readEmvCard(session, iso7816Tag, onScanned, onError)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EMV APDU sequence (iOS)
// ─────────────────────────────────────────────────────────────────────────────

private fun readEmvCard(
    session:   NFCTagReaderSession,
    tag:       NFCISO7816TagProtocol,
    onScanned: (ScannedCard) -> Unit,
    onError:   (NfcScanError) -> Unit
) {
    /** Synchronous APDU exchange using a GCD semaphore (we're already off the main thread). */
    fun send(apdu: ByteArray): ByteArray? {
        val nfcApdu = buildApdu(apdu) ?: return null
        val sem = dispatch_semaphore_create(0)
        var result: ByteArray? = null

        tag.sendCommandAPDU(nfcApdu) { responseData, sw1, sw2, err ->
            if (err == null && responseData != null) {
                // Use memcpy to safely copy NSData bytes into a Kotlin ByteArray
                val len = responseData.length.toInt()
                val buf = ByteArray(len + 2)
                if (len > 0) {
                    buf.usePinned { pinned ->
                        memcpy(pinned.addressOf(0), responseData.bytes, len.toULong())
                    }
                }
                buf[len]     = sw1.toByte()
                buf[len + 1] = sw2.toByte()
                result = buf
            }
            dispatch_semaphore_signal(sem)
        }

        dispatch_semaphore_wait(sem, DISPATCH_TIME_FOREVER)
        return result
    }

    // 1. SELECT PPSE
    val ppseResp = send(EmvParser.SELECT_PPSE) ?: run {
        session.invalidateSessionWithErrorMessage("Read failed.")
        onError(NfcScanError.ParseFailed); return
    }
    if (!EmvParser.isSuccess(ppseResp)) {
        session.invalidateSessionWithErrorMessage("Card read failed.")
        onError(NfcScanError.ParseFailed); return
    }

    val aids = EmvParser.parseAidsFromPpse(EmvParser.stripSW(ppseResp))
    if (aids.isEmpty()) {
        session.invalidateSessionWithErrorMessage("No payment application found.")
        onError(NfcScanError.ParseFailed); return
    }

    // 2. SELECT AID
    var selectResp: ByteArray? = null
    for (aid in aids) {
        val resp = send(EmvParser.buildSelectAid(aid))
        if (resp != null && EmvParser.isSuccess(resp)) { selectResp = resp; break }
    }
    if (selectResp == null) {
        session.invalidateSessionWithErrorMessage("No payment application found.")
        onError(NfcScanError.ParseFailed); return
    }

    // 3. GET PROCESSING OPTIONS
    val gpoResp = send(EmvParser.GET_PROCESSING_OPTIONS)
    val aflBytes: ByteArray? = if (gpoResp != null && EmvParser.isSuccess(gpoResp)) {
        val d = EmvParser.stripSW(gpoResp)
        EmvParser.findTag(d, 0x94)
            ?: EmvParser.findTag(d, 0x80)?.let { f -> if (f.size > 2) f.copyOfRange(2, f.size) else null }
    } else null

    // 4. READ RECORD
    val entries: List<Triple<Int, Int, Int>> = if (aflBytes != null && aflBytes.size % 4 == 0) {
        (aflBytes.indices step 4).mapNotNull { i ->
            val sfi   = (aflBytes[i].toInt() and 0xFF) shr 3
            val first = aflBytes[i + 1].toInt() and 0xFF
            val last  = aflBytes[i + 2].toInt() and 0xFF
            if (sfi in 1..10 && first in 1..10 && last >= first) Triple(sfi, first, last) else null
        }
    } else {
        listOf(Triple(1, 1, 6), Triple(2, 1, 6), Triple(3, 1, 4), Triple(4, 1, 4))
    }

    for ((sfi, first, last) in entries) {
        for (rec in first..last) {
            val resp = send(EmvParser.buildReadRecord(sfi, rec)) ?: continue
            if (!EmvParser.isSuccess(resp)) continue
            val data = EmvParser.stripSW(resp)

            val track2 = EmvParser.findTag(data, 0x57)
            if (track2 != null) {
                val card = EmvParser.parseTrack2(track2)
                if (card?.cardNumber != null) {
                    session.invalidateSession()
                    onScanned(card); return
                }
            }

            val pan5a = EmvParser.findTag(data, 0x5A)
            if (pan5a != null) {
                val pan = EmvParser.parsePanTag5A(pan5a)
                if (pan != null) {
                    val expBytes = EmvParser.findTag(data, 0x5F24)
                    val (mm, yy) = if (expBytes != null && expBytes.size >= 2) {
                        val hex = expBytes.joinToString("") { b -> (b.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }
                        Pair(hex.substring(2, 4), hex.substring(0, 2))
                    } else Pair(null, null)
                    session.invalidateSession()
                    onScanned(ScannedCard(cardNumber = pan, expiryMonth = mm, expiryYear = yy)); return
                }
            }
        }
    }

    session.invalidateSessionWithErrorMessage("Could not read card details.")
    onError(NfcScanError.ParseFailed)
}

// ─────────────────────────────────────────────────────────────────────────────
// APDU builder
// ─────────────────────────────────────────────────────────────────────────────

private fun buildApdu(bytes: ByteArray): NFCISO7816APDU? {
    if (bytes.size < 4) return null
    val cla = (bytes[0].toInt() and 0xFF).toUByte()
    val ins = (bytes[1].toInt() and 0xFF).toUByte()
    val p1  = (bytes[2].toInt() and 0xFF).toUByte()
    val p2  = (bytes[3].toInt() and 0xFF).toUByte()

    return when {
        bytes.size == 4 -> {
            // Case 1: no data, no Le
            NFCISO7816APDU(
                instructionClass = cla, instructionCode = ins,
                p1Parameter = p1, p2Parameter = p2,
                data = NSData(), expectedResponseLength = -1L
            )
        }
        bytes.size == 5 -> {
            // Case 2: no data, Le = bytes[4]
            val le = bytes[4].toInt() and 0xFF
            NFCISO7816APDU(
                instructionClass = cla, instructionCode = ins,
                p1Parameter = p1, p2Parameter = p2,
                data = NSData(),
                expectedResponseLength = if (le == 0) 256L else le.toLong()
            )
        }
        else -> {
            // Case 3/4: Lc = bytes[4], data = bytes[5..5+Lc], optional Le
            val lc = bytes[4].toInt() and 0xFF
            if (bytes.size < 5 + lc) return null
            val body = bytes.copyOfRange(5, 5 + lc)
            val nsData: NSData = body.toNSData()
            val le: Long = if (bytes.size == 5 + lc + 1) {
                val leVal = bytes[5 + lc].toInt() and 0xFF
                if (leVal == 0) 256L else leVal.toLong()
            } else -1L
            NFCISO7816APDU(
                instructionClass = cla, instructionCode = ins,
                p1Parameter = p1, p2Parameter = p2,
                data = nsData, expectedResponseLength = le
            )
        }
    }
}

private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
    }
}
