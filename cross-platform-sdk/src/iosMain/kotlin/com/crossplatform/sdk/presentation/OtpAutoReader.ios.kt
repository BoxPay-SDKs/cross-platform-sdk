package com.crossplatform.sdk.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIControlEventEditingChanged
import platform.UIKit.UIKeyboardTypeNumberPad
import platform.UIKit.UITextContentTypeOneTimeCode
import platform.UIKit.UITextField
import platform.darwin.NSObject

/**
 * iOS has no API for an app to read SMS content. The only integration point
 * is UITextContentType.oneTimeCode, which makes iOS surface an incoming
 * code as a QuickType keyboard suggestion the user taps to autofill —
 * that tap is unavoidable, there's no silent path.
 *
 * Compose Multiplatform doesn't render native UITextFields, so we interop
 * a single 1x1pt hidden UITextField into the view tree purely to receive
 * that autofill event, then forward the value up to Compose state and
 * remove it from view.
 *
 * CRITICAL: autofill does not paste the code atomically — iOS types it into
 * the field character by character, firing onEditingChanged at every
 * intermediate length. onEditingChanged only fires onOtpReceived (and
 * clears the field) once the digit count exactly matches [expectedLength].
 * Firing early on a partial match (e.g. treating a 4-digit prefix of a
 * 6-digit code as complete) both truncates the OTP and — because the field
 * gets cleared immediately after — permanently discards the remaining
 * digits the system was still about to type in.
 *
 * Split into two objects deliberately:
 * - [OtpFieldTarget] is NSObject-only. Its sole job is being the
 *   UIControl target-action receiver — addTarget(target:action:...) keeps
 *   only a WEAK reference to its target, so this has to be something
 *   `remember` keeps alive for the composable's lifetime, not a throwaway
 *   local object inside the factory lambda. Its onEditingChanged() method
 *   is annotated @ObjCAction: without it, plain Kotlin methods on an
 *   NSObject subclass aren't registered as real Objective-C selectors, and
 *   UIKit's target-action dispatch (sendAction:to:from:forEvent:) crashes
 *   with "unrecognized selector" trying to reach them at runtime.
 * - The returned OtpAutoReaderHandle is a plain Kotlin object with no
 *   NSObject involvement at all. Kotlin/Native doesn't allow a single class
 *   to both extend an Objective-C class (NSObject) and implement a plain
 *   Kotlin interface at the same time ("Mixing Kotlin and Objective-C
 *   supertypes is not supported") — so the two roles can't live on one
 *   class. This handle just delegates to the target's UITextField.
 */
@OptIn(ExperimentalForeignApi::class)
private class OtpFieldTarget : NSObject() {
    var textField: UITextField? = null
    var expectedLength: Int = 0
    var onOtpReceived: (String) -> Unit = {}

    @ObjCAction
    fun onEditingChanged() {
        val field = textField ?: return
        val digitsOnly = field.text.orEmpty().filter { it.isDigit() }

        // Not yet complete — let iOS keep typing the remaining digits in.
        // Do NOT clear the field here, or the rest of the autofill is lost.
        if (digitsOnly.length < expectedLength) return

        val code = digitsOnly.take(expectedLength)
        onOtpReceived(code)
        field.text = ""
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberOtpAutoReader(
    expectedLength: Int,
    onOtpReceived: (String) -> Unit
): OtpAutoReaderHandle {
    val target = remember { OtpFieldTarget() }

    // Reassigned every recomposition so the target always uses the latest
    // values, without needing the target itself to be recreated.
    target.onOtpReceived = onOtpReceived
    target.expectedLength = expectedLength

    UIKitView(
        factory = {
            val field = UITextField().apply {
                textContentType = UITextContentTypeOneTimeCode
                keyboardType = UIKeyboardTypeNumberPad
                hidden = true
            }

            field.addTarget(
                target = target,
                action = NSSelectorFromString("onEditingChanged"),
                forControlEvents = UIControlEventEditingChanged
            )

            target.textField = field
            field
        },
        modifier = Modifier.size(1.dp)
    )

    return remember(target) {
        object : OtpAutoReaderHandle {
            override fun start() {
                target.textField?.becomeFirstResponder()
            }

            override fun stop() {
                target.textField?.resignFirstResponder()
            }
        }
    }
}