package com.crossplatform.sdk.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.crossplatform.sdk.presentation.components.scanner.NfcAvailability
import com.crossplatform.sdk.presentation.components.scanner.NfcScanError
import com.crossplatform.sdk.presentation.components.scanner.ScannedCard
import com.crossplatform.sdk.presentation.components.scanner.rememberNfcAvailability
import com.crossplatform.sdk.presentation.components.scanner.NfcReaderEffect
import com.crossplatform.sdk.presentation.theme.LocalSDKColors
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.nfc_scan_cancel
import crossplatformsdk.cross_platform_sdk.generated.resources.nfc_scan_disabled
import crossplatformsdk.cross_platform_sdk.generated.resources.nfc_scan_hint
import crossplatformsdk.cross_platform_sdk.generated.resources.nfc_scan_not_supported
import crossplatformsdk.cross_platform_sdk.generated.resources.nfc_scan_title
import org.jetbrains.compose.resources.stringResource

/**
 * Full-screen NFC card scan dialog.
 *
 * Shows a pulsing phone+card illustration and a "Hold your NFC card to the back of your device"
 * prompt.  [NfcReaderEffect] is composed inside so the platform NFC session is live for the
 * lifetime of this dialog.
 *
 * On iOS, the system NFC sheet slides up on top of this dialog; the user still sees our
 * Cancel button and can dismiss before the system sheet appears.
 */
@Composable
internal fun NfcScanDialog(
    accentColor: Color,
    onDismiss: () -> Unit,
    onScanned: (ScannedCard) -> Unit
) {
    val nfcAvailability = rememberNfcAvailability()
    val fontFamily = LocalSDKFonts.current.primary
    val bg = LocalSDKColors.current.background

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
        ) {
            when (nfcAvailability) {
                NfcAvailability.Available -> {
                    // Active reader effect — works for the dialog's lifetime
                    NfcReaderEffect(
                        onScanned = { card ->
                            onDismiss()
                            onScanned(card)
                        },
                        onError = { onDismiss() }
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        PulsingNfcIcon(accentColor = accentColor)

                        Spacer(Modifier.height(32.dp))

                        Text(
                            text = stringResource(Res.string.nfc_scan_title),
                            color = bg,
                            fontFamily = fontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = stringResource(Res.string.nfc_scan_hint),
                            color = bg.copy(alpha = 0.75f),
                            fontFamily = fontFamily,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                NfcAvailability.Disabled -> {
                    Text(
                        text = stringResource(Res.string.nfc_scan_disabled),
                        color = bg,
                        fontFamily = fontFamily,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp)
                    )
                }

                NfcAvailability.NotSupported -> {
                    Text(
                        text = stringResource(Res.string.nfc_scan_not_supported),
                        color = bg,
                        fontFamily = fontFamily,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 32.dp)
                    )
                }
            }

            // Cancel button — always visible
            Text(
                text = stringResource(Res.string.nfc_scan_cancel),
                color = bg,
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .clickable { onDismiss() }
                    .padding(16.dp)
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Pulsing NFC icon drawn with Canvas — no image asset required
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun PulsingNfcIcon(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "nfc_pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "nfc_pulse_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(120.dp)
    ) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .size(120.dp)
                .alpha((1f - pulse).coerceIn(0f, 1f))
                .drawBehind {
                    val radius = size.minDimension / 2f * pulse
                    drawCircle(
                        color = accentColor.copy(alpha = 0.25f),
                        radius = radius,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
        )

        // Phone card graphic
        Box(
            modifier = Modifier
                .size(72.dp, 56.dp)
                .drawBehind {
                    val stroke = 2.dp.toPx()
                    val corner = CornerRadius(8.dp.toPx())

                    // Phone outline
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(size.width * 0.35f, 0f),
                        size = Size(size.width * 0.65f, size.height),
                        cornerRadius = corner,
                        style = Stroke(width = stroke)
                    )

                    // Card outline (slightly offset to show tap gesture)
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(0f, size.height * 0.15f),
                        size = Size(size.width * 0.55f, size.height * 0.7f),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                        style = Stroke(width = stroke)
                    )

                    // NFC waves (3 arcs)
                    val cx = size.width * 0.68f
                    val cy = size.height * 0.5f
                    for (ring in 1..3) {
                        val r = ring * 8.dp.toPx()
                        drawArc(
                            color = accentColor.copy(alpha = 0.7f),
                            startAngle = -50f,
                            sweepAngle = 100f,
                            useCenter = false,
                            topLeft = Offset(cx - r, cy - r),
                            size = Size(r * 2, r * 2),
                            style = Stroke(width = stroke)
                        )
                    }
                }
        )
    }
}
