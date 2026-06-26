package com.crossplatform

import androidx.compose.ui.window.ComposeUIViewController
import com.crossplatform.sdk.BoxPayCommonCheckout
import com.crossplatform.sdk.data.handler.CommonSDKDismissHandler

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
    fontFamily : String?
) = ComposeUIViewController {
    CommonSDKDismissHandler.setCloseSDK { onDismiss() }
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
        fontFamily = fontFamily
    )
}