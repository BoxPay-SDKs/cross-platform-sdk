package com.crossplatform.sdk.presentation.components.scanner

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Unwraps ContextWrapper chains to find the host Activity. */
private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

// ─────────────────────────────────────────────────────────────────────────────
// NFC availability
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal actual fun rememberNfcAvailability(): NfcAvailability {
    val context = LocalContext.current
    return remember {
        val adapter = NfcAdapter.getDefaultAdapter(context)
        when {
            adapter == null    -> NfcAvailability.NotSupported
            !adapter.isEnabled -> NfcAvailability.Disabled
            else               -> NfcAvailability.Available
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NFC reader effect
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Engages [NfcAdapter.enableReaderMode] for the host Activity while composed.
 * ISO-A and ISO-B cover all mainstream payment card families (Visa, MC, Amex, RuPay, etc.).
 *
 * When a tag arrives, a worker thread runs the EMV APDU sequence on the [IsoDep] channel
 * and reports back on the main thread via [onScanned] or [onError].
 */
@Composable
internal actual fun NfcReaderEffect(
    onScanned: (ScannedCard) -> Unit,
    onError:   (NfcScanError) -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val adapter  = NfcAdapter.getDefaultAdapter(context)

        if (activity == null || adapter == null || !adapter.isEnabled) {
            onError(if (adapter == null) NfcScanError.NotSupported else NfcScanError.Disabled)
            return@DisposableEffect onDispose {}
        }

        val callback = NfcAdapter.ReaderCallback { tag: Tag ->
            readEmvCard(tag, onScanned, onError)
        }

        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK   // skip NDEF, we want ISO 7816

        adapter.enableReaderMode(activity, callback, flags, Bundle())

        onDispose {
            runCatching { adapter.disableReaderMode(activity) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EMV APDU sequence
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Runs the minimal EMV contactless read on a worker thread:
 *   1. SELECT PPSE  → discover AIDs
 *   2. SELECT AID   → select first application
 *   3. GET PROCESSING OPTIONS → obtain AFL
 *   4. READ RECORD (scan all AFL SFI/record combinations) → find tag 57 or 5A
 */
private fun readEmvCard(
    tag:       Tag,
    onScanned: (ScannedCard) -> Unit,
    onError:   (NfcScanError) -> Unit
) {
    val isoDep = IsoDep.get(tag) ?: run { onError(NfcScanError.ParseFailed); return }
    try {
        isoDep.connect()
        isoDep.timeout = 3_000  // 3 s — more than enough for an RF exchange

        // 1. SELECT PPSE
        val ppseResp = isoDep.transceive(EmvParser.SELECT_PPSE)
        if (!EmvParser.isSuccess(ppseResp)) { onError(NfcScanError.ParseFailed); return }

        val aids = EmvParser.parseAidsFromPpse(EmvParser.stripSW(ppseResp))
        if (aids.isEmpty()) { onError(NfcScanError.ParseFailed); return }

        // 2. SELECT AID — try each AID until one succeeds
        var selectResp: ByteArray? = null
        for (aid in aids) {
            val resp = isoDep.transceive(EmvParser.buildSelectAid(aid))
            if (EmvParser.isSuccess(resp)) { selectResp = resp; break }
        }
        if (selectResp == null) { onError(NfcScanError.ParseFailed); return }

        // 3. GET PROCESSING OPTIONS (we send an empty PDOL response)
        val gpoResp = isoDep.transceive(EmvParser.GET_PROCESSING_OPTIONS)
        // GPO can fail on some cards without crashing the read — we still attempt record reads
        val aflBytes: ByteArray? = if (EmvParser.isSuccess(gpoResp)) {
            val gpoData = EmvParser.stripSW(gpoResp)
            // Format 1 (80) or Format 2 (77) — AFL is tag 94
            EmvParser.findTag(gpoData, 0x94)
                ?: EmvParser.findTag(gpoData, 0x80)?.let { fmt1 ->
                    // In format 1: bytes 3+ are the AFL
                    if (fmt1.size > 2) fmt1.copyOfRange(2, fmt1.size) else null
                }
        } else null

        // 4. READ RECORD
        val card = readRecords(isoDep, aflBytes)
        if (card != null) {
            onScanned(card)
        } else {
            onError(NfcScanError.ParseFailed)
        }
    } catch (_: Exception) {
        onError(NfcScanError.TagLost)
    } finally {
        runCatching { isoDep.close() }
    }
}

/**
 * Reads EMV records following the AFL (Application File Locator) when present,
 * otherwise brute-forces SFI 1-4, records 1-8 which covers virtually all payment cards.
 */
private fun readRecords(isoDep: IsoDep, aflBytes: ByteArray?): ScannedCard? {
    // Parse AFL into (sfi, firstRecord, lastRecord) triples if we have one
    val entries: List<Triple<Int, Int, Int>> = if (aflBytes != null && aflBytes.size % 4 == 0) {
        (aflBytes.indices step 4).mapNotNull { i ->
            val sfi    = (aflBytes[i].toInt() and 0xFF) shr 3
            val first  = aflBytes[i + 1].toInt() and 0xFF
            val last   = aflBytes[i + 2].toInt() and 0xFF
            if (sfi in 1..10 && first in 1..10 && last >= first) Triple(sfi, first, last) else null
        }
    } else {
        // Fallback — common SFI/record ranges for Visa/MC/Amex/RuPay
        listOf(
            Triple(1, 1, 6),
            Triple(2, 1, 6),
            Triple(3, 1, 4),
            Triple(4, 1, 4)
        )
    }

    for ((sfi, first, last) in entries) {
        for (rec in first..last) {
            val resp = runCatching { isoDep.transceive(EmvParser.buildReadRecord(sfi, rec)) }
                .getOrNull() ?: continue
            if (!EmvParser.isSuccess(resp)) continue

            val data = EmvParser.stripSW(resp)

            // Prefer Track 2 Equivalent Data (tag 57) — has PAN + expiry
            val track2 = EmvParser.findTag(data, 0x57)
            if (track2 != null) {
                val card = EmvParser.parseTrack2(track2)
                if (card?.cardNumber != null) return card
            }

            // Fallback: plain PAN tag 5A (no expiry here, but still useful)
            val pan5a = EmvParser.findTag(data, 0x5A)
            if (pan5a != null) {
                val pan = EmvParser.parsePanTag5A(pan5a)
                if (pan != null) {
                    // Try to also grab expiry from tag 5F24 in the same record
                    val expBytes = EmvParser.findTag(data, 0x5F24)
                    val (mm, yy) = if (expBytes != null && expBytes.size >= 2) {
                        val hex = expBytes.joinToString("") { "%02X".format(it.toInt() and 0xFF) }
                        Pair(hex.substring(2, 4), hex.substring(0, 2))  // YYMMDD → mm=2-4, yy=0-2
                    } else Pair(null, null)
                    return ScannedCard(cardNumber = pan, expiryMonth = mm, expiryYear = yy)
                }
            }
        }
    }
    return null
}