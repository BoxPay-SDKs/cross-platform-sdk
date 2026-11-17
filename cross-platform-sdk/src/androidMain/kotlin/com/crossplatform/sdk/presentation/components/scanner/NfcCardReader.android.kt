package com.crossplatform.sdk.presentation.components.scanner

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.IOException

private const val TAG = "NfcCardReader"

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

private fun currentNfcAvailability(context: Context): NfcAvailability {
    val adapter = NfcAdapter.getDefaultAdapter(context)
    return when {
        adapter == null    -> NfcAvailability.NotSupported
        !adapter.isEnabled -> NfcAvailability.Disabled
        else               -> NfcAvailability.Available
    }
}

/**
 * Re-checked every time the screen resumes, so turning NFC on or off in Settings and
 * coming back is picked up without leaving the checkout.
 */
@Composable
internal actual fun rememberNfcAvailability(): NfcAvailability {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var availability by remember { mutableStateOf(currentNfcAvailability(context)) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) availability = currentNfcAvailability(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return availability
}

// ─────────────────────────────────────────────────────────────────────────────
// NFC reader effect
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Engages [NfcAdapter.enableReaderMode] for the host Activity while composed and resumed.
 * ISO-A and ISO-B cover all mainstream payment card families (Visa, MC, Amex, RuPay, etc.).
 *
 * Reader mode is switched on at ON_RESUME and off at ON_PAUSE, so it is active again after
 * the app was backgrounded or the user came back from Settings.
 *
 * When a tag arrives, the EMV APDU sequence runs on the NFC binder thread that delivered it
 * (blocking is allowed there). The result is then posted to the main looper, so [onScanned]
 * and [onError] always run on the main thread.
 */
@Composable
internal actual fun NfcReaderEffect(
    onScanned: (ScannedCard) -> Unit,
    onError:   (NfcScanError) -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Keep the latest lambdas without restarting the NFC session when they change.
    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnError   by rememberUpdatedState(onError)

    DisposableEffect(lifecycleOwner, context) {
        val activity    = context.findActivity()
        val adapter     = NfcAdapter.getDefaultAdapter(context)
        val mainHandler = Handler(Looper.getMainLooper())
        var readerEnabled = false

        val callback = NfcAdapter.ReaderCallback { tag: Tag ->
            // Runs on a binder thread — hop to main before touching Compose state.
            readEmvCard(
                tag       = tag,
                onScanned = { card -> mainHandler.post { currentOnScanned(card) } },
                onError   = { err  -> mainHandler.post { currentOnError(err) } }
            )
        }

        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK   // skip NDEF, we want ISO 7816

        fun enable() {
            if (readerEnabled) return
            if (activity == null || adapter == null) {
                mainHandler.post { currentOnError(NfcScanError.NotSupported) }
                return
            }
            if (!adapter.isEnabled) {
                mainHandler.post { currentOnError(NfcScanError.Disabled) }
                return
            }
            adapter.enableReaderMode(activity, callback, flags, Bundle())
            readerEnabled = true
        }

        fun disable() {
            if (!readerEnabled || activity == null || adapter == null) return
            runCatching { adapter.disableReaderMode(activity) }
            readerEnabled = false
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> enable()
                Lifecycle.Event.ON_PAUSE  -> disable()
                else -> Unit
            }
        }
        // addObserver replays the current state, so this also enables reader mode right away
        // when the screen is already resumed.
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            disable()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EMV read
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Runs the shared [EmvCardReader] flow over the tag's [IsoDep] channel.
 * Called on the NFC binder thread; the callbacks are invoked on that same thread.
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

        val card = EmvCardReader.read { apdu -> isoDep.transceive(apdu) }
        if (card != null) onScanned(card) else onError(NfcScanError.ParseFailed)
    } catch (_: IOException) {
        // TagLostException is an IOException: card moved away or the RF link dropped.
        onError(NfcScanError.TagLost)
    } catch (e: Exception) {
        // Log the type only — never card data.
        Log.w(TAG, "EMV read failed: ${e::class.simpleName}")
        onError(NfcScanError.ParseFailed)
    } finally {
        runCatching { isoDep.close() }
    }
}
