package com.crossplatform.sdk.presentation.components.scanner

import androidx.compose.runtime.Composable

/** Reasons the NFC scan could not complete. */
internal enum class NfcScanError {
    /** Device has no NFC hardware. */
    NotSupported,
    /** NFC hardware is present but switched off by the user. */
    Disabled,
    /** Card moved away before the read finished. */
    TagLost,
    /** Card was read but the data couldn't be parsed into a valid PAN. */
    ParseFailed,
    /** User cancelled the scan. */
    Cancelled
}

/**
 * Platform-specific composable effect that runs an NFC card-reading session for as long as
 * it is composed.  Calls [onScanned] (on the main thread) when a valid card is detected, or
 * [onError] when the session ends without a result.
 *
 * Android: engages [NfcAdapter.enableReaderMode] on the host Activity.
 * iOS: starts an [NFCTagReaderSession] which presents the system NFC sheet.
 */
@Composable
internal expect fun NfcReaderEffect(
    onScanned: (ScannedCard) -> Unit,
    onError:   (NfcScanError) -> Unit
)

/**
 * Returns whether NFC is available and enabled on this device.
 * Calling this is cheap — no I/O — so it is safe to call from composition.
 */
@Composable
internal expect fun rememberNfcAvailability(): NfcAvailability

internal enum class NfcAvailability {
    /** NFC is present and switched on. */
    Available,
    /** Device has no NFC hardware. */
    NotSupported,
    /** NFC hardware is present but switched off in device Settings. */
    Disabled
}
