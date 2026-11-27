package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.crossplatform.sdk.presentation.sharedContext.AutoRefundHandler
import com.crossplatform.sdk.presentation.theme.LocalSDKColors
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.confirming_payment_info
import crossplatformsdk.cross_platform_sdk.generated.resources.loading_info
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun ShowLoadingComponent(modifier: Modifier) {
    val secondsLeft by AutoRefundHandler.secondsLeft.collectAsStateWithLifecycle()
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/BoxPayLogo.json").decodeToString()
        )
    }
    Column(
        modifier            = modifier
            .background(LocalSDKColors.current.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberLottiePainter(
                composition = composition,
                iterations = Compottie.IterateForever
            ),
            contentDescription = "Lottie animation",
            modifier = Modifier.size(96.dp)
        )
        Text(
            text       = stringResource(Res.string.loading_info),
            fontFamily = LocalSDKFonts.current.primary,
            fontWeight = FontWeight.Normal,
            fontSize   = 14.sp,
            color = LocalSDKColors.current.textPrimary
        )
        // Auto-refund countdown, shown just below the loader
        val left = secondsLeft
        if (left != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text       = stringResource(Res.string.confirming_payment_info),
                fontFamily = LocalSDKFonts.current.primary,
                fontWeight = FontWeight.Normal,
                fontSize   = 14.sp,
                color = LocalSDKColors.current.textSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = formatCountdown(left),
                fontFamily = LocalSDKFonts.current.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 22.sp,
                color = LocalSDKColors.current.textPrimary
            )
        }
    }
}

private fun formatCountdown(totalSeconds: Int): String {
    val minutes = (totalSeconds / 60).toString().padStart(2, '0')
    val seconds = (totalSeconds % 60).toString().padStart(2, '0')
    return "$minutes:$seconds"
}