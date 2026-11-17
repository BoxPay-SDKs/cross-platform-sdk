@file:OptIn(ExperimentalForeignApi::class)

package com.crossplatform.sdk.presentation.components.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreNFC.NFCISO7816APDU
import platform.CoreNFC.NFCISO7816TagProtocol
import platform.CoreNFC.NFCReaderSessionInvalidationErrorSessionTimeout
import platform.CoreNFC.NFCReaderSessionInvalidationErrorUserCanceled
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
 * EMV APDU commands run on a background dispatch queue; session callbacks and the
 * [onScanned] / [onError] callbacks all run on the main queue, and exactly one of them is
 * delivered per session.
 *
 * Host app requirements:
 *   • `NFCReaderUsageDescription` in Info.plist
 *   • Entitlement `com.apple.developer.nfc.readersession.formats` = [ "TAG" ]
 *   • `com.apple.developer.nfc.readersession.iso7816.select-identifiers` in Info.plist,
 *     listing every application ID this reader may SELECT. CoreNFC rejects a SELECT for an
 *     AID that is not listed, so the array must contain at least:
 *       - `325041592E5359532E4444463031`  (PPSE, "2PAY.SYS.DDF01")
 *       - the payment AIDs to be read, e.g. Visa `A0000000031010`, Mastercard `A0000000041010`,
 *         Maestro `A0000000043060`, Amex `A00000002501`, RuPay `A0000005241010`
 *     A card whose AID is not in the list will fail with ParseFailed.
 */
@Composable
internal actual fun NfcReaderEffect(
    onScanned: (ScannedCard) -> Unit,
    onError:   (NfcScanError) -> Unit
) {
    // Keep the latest lambdas without restarting the session when they change.
    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnError   by rememberUpdatedState(onError)

    DisposableEffect(Unit) {
        val delegate = NfcSessionDelegate(
            onScanned = { card -> currentOnScanned(card) },
            onError   = { err  -> currentOnError(err) }
        )

        // pollingOption 0x01 = ISO 14443 (type A and B, which includes ISO-DEP payment cards).
        // 0x03 would also enable ISO 15693, which payment cards never use.
        val session = NFCTagReaderSession(
            pollingOption = 0x01L,
            delegate      = delegate,
            queue         = dispatch_get_main_queue()   // delegate callbacks on main
        ) ?: run {
            currentOnError(NfcScanError.NotSupported)
            return@DisposableEffect onDispose {}
        }

        session.alertMessage = NfcMessages.HOLD_CARD
        session.beginSession()

        onDispose {
            runCatching { session.invalidateSession() }
        }
    }
}

// Hard-coded for now: these are shown in the system NFC sheet, outside Compose.
// To localise them, resolve stringResource(...) in NfcReaderEffect's caller and pass them in.
private object NfcMessages {
    const val HOLD_CARD     = "Hold your card to the top of your iPhone."
    const val CONNECT_FAIL  = "Connection failed."
    const val NOT_SUPPORTED = "Card type not supported."
    const val READ_FAIL     = "Could not read card details."
}

// ─────────────────────────────────────────────────────────────────────────────
// Session delegate
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Reports exactly one result. Invalidating a session (ours, or the system's on cancel /
 * timeout) also fires didInvalidateWithError, so without a guard a failure — or even a
 * success followed by invalidateSession() — would reach the caller twice.
 */
private class NfcSessionDelegate(
    private val onScanned: (ScannedCard) -> Unit,
    private val onError:   (NfcScanError) -> Unit
) : NSObject(), NFCTagReaderSessionDelegateProtocol {

    /** Read and written only on the main queue, so no locking is needed. */
    private var finished = false

    fun deliverScanned(card: ScannedCard) {
        dispatch_async(dispatch_get_main_queue()) {
            if (!finished) {
                finished = true
                onScanned(card)
            }
        }
    }

    fun deliverError(error: NfcScanError) {
        dispatch_async(dispatch_get_main_queue()) {
            if (!finished) {
                finished = true
                onError(error)
            }
        }
    }

    /**
     * Report first, invalidate second: both hops go through the main queue, so our result is
     * already recorded when the resulting didInvalidateWithError arrives and is ignored.
     */
    fun fail(session: NFCTagReaderSession, message: String, error: NfcScanError) {
        deliverError(error)
        session.invalidateSessionWithErrorMessage(message)
    }

    fun succeed(session: NFCTagReaderSession, card: ScannedCard) {
        deliverScanned(card)
        session.invalidateSession()
    }

    override fun tagReaderSessionDidBecomeActive(session: NFCTagReaderSession) { /* ready */ }

    override fun tagReaderSession(
        session: NFCTagReaderSession,
        didInvalidateWithError: platform.Foundation.NSError
    ) {
        val code = didInvalidateWithError.code
        if (code == NFCReaderSessionInvalidationErrorUserCanceled.toLong() ||
            code == NFCReaderSessionInvalidationErrorSessionTimeout.toLong()
        ) {
            deliverError(NfcScanError.Cancelled)
        } else {
            deliverError(NfcScanError.TagLost)
        }
    }

    override fun tagReaderSession(
        session: NFCTagReaderSession,
        didDetectTags: List<*>
    ) {
        val tag = didDetectTags.firstOrNull() as? NFCTagProtocol ?: return

        session.connectToTag(tag) { error ->
            if (error != null) {
                fail(session, NfcMessages.CONNECT_FAIL, NfcScanError.TagLost)
                return@connectToTag
            }

            val iso7816Tag = tag.asNFCISO7816Tag()
            if (iso7816Tag == null) {
                fail(session, NfcMessages.NOT_SUPPORTED, NfcScanError.ParseFailed)
                return@connectToTag
            }

            dispatch_async(
                dispatch_get_global_queue(QOS_CLASS_USER_INITIATED.toLong(), 0UL)
            ) {
                readEmvCard(session, iso7816Tag, this@NfcSessionDelegate)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EMV read (iOS)
// ─────────────────────────────────────────────────────────────────────────────

/** Runs the shared [EmvCardReader] flow. Called off the main thread (it blocks on each APDU). */
private fun readEmvCard(
    session:  NFCTagReaderSession,
    tag:      NFCISO7816TagProtocol,
    delegate: NfcSessionDelegate
) {
    val card = EmvCardReader.read { apdu -> sendApdu(tag, apdu) }
    if (card != null) {
        delegate.succeed(session, card)
    } else {
        delegate.fail(session, NfcMessages.READ_FAIL, NfcScanError.ParseFailed)
    }
}

/**
 * Synchronous APDU exchange using a GCD semaphore (we are already off the main thread).
 * Returns the response data followed by SW1 SW2, or null when the exchange failed.
 */
private fun sendApdu(tag: NFCISO7816TagProtocol, apdu: ByteArray): ByteArray? {
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
