package com.crossplatform

import androidx.compose.ui.window.ComposeUIViewController
import com.crossplatform.sdk.BoxPayCommonCheckout
import com.crossplatform.sdk.data.handler.CommonSDKDismissHandler
import com.crossplatform.sdk.presentation.components.LocaleManager
import com.crossplatform.sdk.presentation.components.createSettings
import com.crossplatform.sdk.presentation.theme.BoxPayThemeMode

/**
 * KMP entry point consumed from Swift / Objective-C.
 *
 * @param themeMode Controls the colour scheme the SDK uses.
 *   Pass [BoxPayThemeMode.LIGHT], [BoxPayThemeMode.DARK], or
 *   [BoxPayThemeMode.SYSTEM] (default – follows the device dark-mode setting).
 *   Use [BoxPayThemeMode.LIGHT] or [BoxPayThemeMode.DARK] when your app manages
 *   its own theme and does not rely on the system setting.
 */
fun BoxPayViewController(
    token : String,
    isTestEnv: Boolean,
    shopperToken: String?,
    isSuccessScreenVisible: Boolean,
    isFailedScreenVisible: Boolean,
    showQROnLoad: Boolean,
    ctaBorderRadius: Int,
    isSICheckBoxChecked: Boolean,
    isSICheckBoxEnabled: Boolean,
    focusedTextInputBorderColor: String,
    unfocusedTextInputBorderColor: String,
    onDismiss: () -> Unit,
    fontFamily : String?,
    themeMode: BoxPayThemeMode = BoxPayThemeMode.SYSTEM,
) = ComposeUIViewController {
    CommonSDKDismissHandler.setCloseSDK { onDismiss() }
    val localeManager = LocaleManager(createSettings())
    BoxPayCommonCheckout(
        token = token,
        isTestEnv = isTestEnv,
        shopperToken = shopperToken,
        isSuccessScreenVisible = isSuccessScreenVisible,
        isFailedScreenVisible = isFailedScreenVisible,
        showQROnLoad = showQROnLoad,
        ctaBorderRadius = ctaBorderRadius,
        isSICheckBoxChecked = isSICheckBoxChecked,
        isSICheckBoxEnabled = isSICheckBoxEnabled,
        focusedTextInputBorderColor = focusedTextInputBorderColor,
        unfocusedTextInputBorderColor = unfocusedTextInputBorderColor,
        fontFamily = fontFamily,
        localeManager = localeManager,
        themeMode = themeMode,
    )
}
