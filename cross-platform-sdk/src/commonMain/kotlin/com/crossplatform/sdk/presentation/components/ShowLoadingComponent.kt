package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.crossplatform.sdk.presentation.theme.defaultFontFamily
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ShowLoadingComponent() {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/BoxPayLogo.json").decodeToString()
        )
    }
    Column(
        modifier            = Modifier.fillMaxSize()
            .background(Color.White),
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
            text       = "Loading...",
            fontFamily = defaultFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize   = 14.sp
        )
    }
}