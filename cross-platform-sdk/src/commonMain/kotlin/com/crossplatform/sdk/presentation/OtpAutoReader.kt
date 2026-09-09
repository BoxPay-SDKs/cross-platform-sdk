package com.crossplatform.sdk.presentation

import androidx.compose.runtime.Composable

/**
 * Handle returned by [rememberOtpAutoReader]. Call [start] once the OTP
 * sheet is shown, [stop] when it's dismissed (e.g. from a DisposableEffect).
 */
interface OtpAutoReaderHandle {
    fun start()
    fun stop()
}

/**
 * Starts listening for an incoming OTP using whatever mechanism the
 * platform supports, and invokes [onOtpReceived] at most once per detected
 * code with the extracted digits.
 *
 * [expectedLength] is the OTP's full length (e.g. maxOtpField). This matters
 * most on iOS: autofill doesn't paste the code atomically, it types it in
 * character-by-character, firing edit events at every intermediate length.
 * Without gating on the exact expected length, a 6-digit code gets accepted
 * (and the field cleared) the moment it hits 4 digits, permanently losing
 * the rest. Android's SMS User Consent API returns the full message in one
 * shot, so this mostly protects against messages containing other
 * incidental numbers (reference IDs, etc.) rather than partial-fill races.
 *
 * - Android: Play Services SMS User Consent API. Fires after the user taps
 *   "Allow" on the system prompt.
 * - iOS: fires when the user taps the QuickType "oneTimeCode" suggestion
 *   above the keyboard, via a hidden interop UITextField.
 */
@Composable
expect fun rememberOtpAutoReader(
    expectedLength: Int,
    onOtpReceived: (String) -> Unit
): OtpAutoReaderHandle

/**
 * Pulls digits out of a raw SMS/message body. Prefers a run that matches
 * [expectedLength] exactly (to disambiguate from other numbers in the
 * message, e.g. reference IDs); falls back to any 4–8 digit run if no exact
 * match is found.
 */
internal fun extractOtpDigits(message: String, expectedLength: Int): String? {
    val exact = Regex("""\b\d{$expectedLength}\b""").find(message)?.value
    if (exact != null) return exact
    return Regex("""\b\d{4,8}\b""").find(message)?.value
}