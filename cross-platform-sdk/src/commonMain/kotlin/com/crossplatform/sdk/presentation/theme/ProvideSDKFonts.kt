package com.crossplatform.sdk.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import crossplatformsdk.cross_platform_sdk.generated.resources.NotoSans_Bold
import crossplatformsdk.cross_platform_sdk.generated.resources.NotoSans_ExtraBold
import crossplatformsdk.cross_platform_sdk.generated.resources.NotoSans_Medium
import crossplatformsdk.cross_platform_sdk.generated.resources.NotoSans_Regular
import crossplatformsdk.cross_platform_sdk.generated.resources.NotoSans_SemiBold
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.arial_bold
import crossplatformsdk.cross_platform_sdk.generated.resources.arial_medium
import crossplatformsdk.cross_platform_sdk.generated.resources.arial_regular
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

@Composable
fun ProvideSDKFonts(
    merchantFont: String? = null,
    backendFont: String? = null,
    onUnknownFontRequested: ((String) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val catalog = fontCatalog()
    val poppins = catalog.getValue("poppins")
    val inter = catalog.getValue("inter")
    val default = SDKFonts(primary = poppins, secondary = inter)

    val fonts = remember(merchantFont, backendFont, catalog) {
        // First non-blank source in priority order.
        val requestedName = merchantFont?.takeIf { it.isNotBlank() }
            ?: backendFont?.takeIf { it.isNotBlank() }

        val family = requestedName?.let { catalog[it.lowercase()] }

        // A name was asked for but isn't bundled → report so it can be logged.
        if (requestedName != null && family == null) {
            onUnknownFontRequested?.invoke(requestedName)
        }

        if (family != null) SDKFonts(primary = family, secondary = inter) else default
    }

    CompositionLocalProvider(LocalSDKFonts provides fonts) {
        content()
    }
}

data class SDKFonts(
    val primary: FontFamily,
    val secondary: FontFamily,
)

@Composable
private fun fontCatalog(): Map<String, FontFamily> = mapOf(
    "poppins" to bundledPoppins(),
    "inter" to bundledInter(),
    "notosans" to bundledNotoSans(),
    "arial" to bundledArial()
)

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun bundledPoppins(): FontFamily = FontFamily(
    Font(Res.font.poppins_regular, FontWeight.Normal),
    Font(Res.font.poppins_medium, FontWeight.Medium),
    Font(Res.font.poppins_semibold, FontWeight.SemiBold),
    Font(Res.font.poppins_bold, FontWeight.Bold),
    Font(Res.font.poppins_extrabold, FontWeight.ExtraBold),
)

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun bundledArial(): FontFamily = FontFamily(
    Font(Res.font.arial_regular, FontWeight.Normal),
    Font(Res.font.arial_medium, FontWeight.Medium),
    Font(Res.font.arial_bold, FontWeight.Bold)
)

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun bundledInter(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, FontWeight.Normal),
    Font(Res.font.inter_medium, FontWeight.Medium),
    Font(Res.font.inter_semibold, FontWeight.SemiBold),
    Font(Res.font.inter_bold, FontWeight.Bold),
    Font(Res.font.inter_extrabold, FontWeight.ExtraBold),
)

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun bundledNotoSans(): FontFamily = FontFamily(
    Font(Res.font.NotoSans_Regular, FontWeight.Normal),
    Font(Res.font.NotoSans_Medium, FontWeight.Medium),
    Font(Res.font.NotoSans_SemiBold, FontWeight.SemiBold),
    Font(Res.font.NotoSans_Bold, FontWeight.Bold),
    Font(Res.font.NotoSans_ExtraBold, FontWeight.ExtraBold),
)


val LocalSDKFonts = staticCompositionLocalOf<SDKFonts> {
    error("SDKFonts not provided")
}