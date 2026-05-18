package com.crossplatform.sdk.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.inter_bold
import crossplatformsdk.cross_platform_sdk.generated.resources.inter_extrabold
import crossplatformsdk.cross_platform_sdk.generated.resources.inter_medium
import crossplatformsdk.cross_platform_sdk.generated.resources.inter_regular
import crossplatformsdk.cross_platform_sdk.generated.resources.inter_semibold
import crossplatformsdk.cross_platform_sdk.generated.resources.poppins_bold
import crossplatformsdk.cross_platform_sdk.generated.resources.poppins_extrabold
import crossplatformsdk.cross_platform_sdk.generated.resources.poppins_medium
import crossplatformsdk.cross_platform_sdk.generated.resources.poppins_regular
import crossplatformsdk.cross_platform_sdk.generated.resources.poppins_semibold
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ProvideSDKFonts(content: @Composable () -> Unit) {

    val poppins = FontFamily(
        Font(Res.font.poppins_regular, FontWeight.Normal),
        Font(Res.font.poppins_medium, FontWeight.Medium),
        Font(Res.font.poppins_semibold, FontWeight.SemiBold),
        Font(Res.font.poppins_bold, FontWeight.Bold),
        Font(Res.font.poppins_extrabold, FontWeight.ExtraBold)
    )
    val inter = FontFamily(
        Font(Res.font.inter_regular, FontWeight.Normal),
        Font(Res.font.inter_medium, FontWeight.Medium),
        Font(Res.font.inter_semibold, FontWeight.SemiBold),
        Font(Res.font.inter_bold, FontWeight.Bold),
        Font(Res.font.inter_extrabold, FontWeight.ExtraBold)
    )

    defaultFontFamily = poppins
    defaultInterFontFamily = inter

    CompositionLocalProvider(LocalSDKFonts provides SDKFonts(poppins, inter)) {
        content()
    }
}

data class SDKFonts(
    val poppins: FontFamily,
    val inter: FontFamily
)

var defaultFontFamily: FontFamily = FontFamily.Default        // poppins
var defaultInterFontFamily: FontFamily = FontFamily.Default   // inter


val LocalSDKFonts = staticCompositionLocalOf<SDKFonts> {
    error("SDKFonts not provided")
}