package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.crossplatform.sdk.presentation.components.scanner.CardScanAggregator
import com.crossplatform.sdk.presentation.components.scanner.CardScanError
import com.crossplatform.sdk.presentation.components.scanner.CardTextParser
import com.crossplatform.sdk.presentation.components.scanner.ScannedCard
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.scan_card_camera_unavailable
import crossplatformsdk.cross_platform_sdk.generated.resources.scan_card_cancel
import crossplatformsdk.cross_platform_sdk.generated.resources.scan_card_hint
import crossplatformsdk.cross_platform_sdk.generated.resources.scan_card_permission_denied
import org.jetbrains.compose.resources.stringResource
import com.crossplatform.sdk.presentation.theme.LocalSDKColors

/**
 * Platform camera + on-device text recognition.
 * Calls [onTextRecognized] on the main thread with the text lines found in a frame,
 * and [onError] if the camera can't be used. Images are never stored or sent anywhere.
 */
@Composable
internal expect fun CameraTextPreview(
    modifier: Modifier,
    onTextRecognized: (List<String>) -> Unit,
    onError: (CardScanError) -> Unit
)

/** Full-screen scanner: camera feed, card-shaped cut-out, cancel button. */
@Composable
internal fun CardScanDialog(
    accentColor: Color,
    onDismiss: () -> Unit,
    onScanned: (ScannedCard) -> Unit
) {
    val aggregator = remember { CardScanAggregator() }
    var error by remember { mutableStateOf<CardScanError?>(null) }
    val fontFamily = LocalSDKFonts.current.primary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (error == null) {
                CameraTextPreview(
                    modifier = Modifier.fillMaxSize(),
                    onTextRecognized = { lines ->
                        aggregator.accept(CardTextParser.parse(lines))?.let(onScanned)
                    },
                    onError = { error = it }
                )

                // Dim everything except a credit-card shaped window (ISO/IEC 7810 ID-1 = 1.586:1).
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                ) {
                    drawRect(Color.Black.copy(alpha = 0.6f))
                    val w = size.width * 0.88f
                    val h = w / 1.586f
                    val topLeft = Offset((size.width - w) / 2f, (size.height - h) / 2f - size.height * 0.05f)
                    val radius = CornerRadius(16.dp.toPx())
                    drawRoundRect(Color.Transparent, topLeft, Size(w, h), radius, blendMode = BlendMode.Clear)
                    drawRoundRect(accentColor, topLeft, Size(w, h), radius, style = Stroke(width = 3.dp.toPx()))
                }

                Text(
                    text = stringResource(Res.string.scan_card_hint),
                    color = LocalSDKColors.current.background,
                    fontFamily = fontFamily,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 32.dp, end = 32.dp, bottom = 96.dp)
                )
            } else {
                Text(
                    text = stringResource(
                        if (error == CardScanError.PermissionDenied) Res.string.scan_card_permission_denied
                        else Res.string.scan_card_camera_unavailable
                    ),
                    color = LocalSDKColors.current.background,
                    fontFamily = fontFamily,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp)
                )
            }

            Text(
                text = stringResource(Res.string.scan_card_cancel),
                color = LocalSDKColors.current.background,
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