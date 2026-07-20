package com.crossplatform.sdk.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.toComposeColor
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_lock
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentRetryBottomSheet(
    sheetState : SheetState,
    totalSeconds: Int = 5,
    onTimeout: () -> Unit = {},
    buttonColor : String,
    buttonTextColor : String,
    onProceedForward : () -> Unit,
    ctaBorderRadius : Int
) {
    var secondsLeft by remember { mutableStateOf(totalSeconds) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
        onProceedForward()
    }

    val progress by animateFloatAsState(
        targetValue = 1f - (secondsLeft / totalSeconds.toFloat()),
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "retryButtonProgress",
    )

    ModalBottomSheet(
        onDismissRequest = onTimeout,
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 12.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_lock),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )

                // Title
                Text(
                    text = "Retrying your payment securely",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = LocalSDKFonts.current.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = "Payment failed. Retrying with another payment partner. OTP may be required again.",
                fontSize = 14.sp,
                fontFamily = LocalSDKFonts.current.primary,
                lineHeight = 20.sp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Countdown / cancel button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable { onTimeout() }
                    // Faint track in the button color, sits behind the moving fill
                    .background(buttonColor.toComposeColor().copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                // Fill that sweeps from left to right as the countdown progresses
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(ctaBorderRadius))
                        .background(buttonColor.toComposeColor())
                )

                Text(
                    text = "Retrying in ${secondsLeft}s \u2013 Tap to cancel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = LocalSDKFonts.current.primary,
                    color = buttonTextColor.toComposeColor(),
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Footer note
            Text(
                text = "Only one successful charge will be processed.",
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}