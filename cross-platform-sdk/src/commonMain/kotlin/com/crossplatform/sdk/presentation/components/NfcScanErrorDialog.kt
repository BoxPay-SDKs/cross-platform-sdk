package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.crossplatform.sdk.presentation.components.scanner.NfcScanError
import com.crossplatform.sdk.presentation.theme.LocalSDKColors
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.nfc_scan_enter_manually
import crossplatformsdk.cross_platform_sdk.generated.resources.nfc_scan_error
import crossplatformsdk.cross_platform_sdk.generated.resources.nfc_scan_error_tag_lost
import crossplatformsdk.cross_platform_sdk.generated.resources.nfc_scan_try_again
import org.jetbrains.compose.resources.stringResource

/**
 * Shown after an NFC read failed (card moved away, or its data could not be read).
 * [onTryAgain] restarts the NFC scan; [onEnterManually] just closes the dialog, leaving the
 * card form for the user to fill in. Back press / tapping outside behaves like "Enter manually".
 */
@Composable
internal fun NfcScanErrorDialog(
    error: NfcScanError,
    buttonColor: String,
    buttonTextColor: String,
    ctaBorderRadius: Int,
    onTryAgain: () -> Unit,
    onEnterManually: () -> Unit
) {
    val fontFamily = LocalSDKFonts.current.primary
    val accent     = buttonColor.toComposeColor()
    val shape      = RoundedCornerShape(ctaBorderRadius.dp)

    val message = stringResource(
        if (error == NfcScanError.TagLost) Res.string.nfc_scan_error_tag_lost
        else Res.string.nfc_scan_error
    )

    Dialog(
        onDismissRequest = onEnterManually,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = LocalSDKColors.current.background
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = message,
                    fontFamily = fontFamily,
                    fontSize = 14.sp,
                    color = LocalSDKColors.current.textSecondary
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onTryAgain,
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accent,
                        contentColor = buttonTextColor.toComposeColor()
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.nfc_scan_try_again),
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onEnterManually,
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    border = BorderStroke(width = 1.dp, color = accent)
                ) {
                    Text(
                        text = stringResource(Res.string.nfc_scan_enter_manually),
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = accent,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}
