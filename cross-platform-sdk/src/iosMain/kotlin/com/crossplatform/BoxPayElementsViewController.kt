package com.crossplatform

import androidx.compose.ui.window.ComposeUIViewController
import com.crossplatform.sdk.BoxPayCommonElements
import com.crossplatform.sdk.data.handler.BoxPayElementsHandler
import com.crossplatform.sdk.data.handler.CommonSDKDismissHandler
import com.crossplatform.sdk.domain.model.PaymentMethodTab

fun BoxPayElementsViewController(
    token : String,
    isTestEnv: Boolean,
    shopperToken: String? = null,
    showQROnLoad: Boolean = false,
    ctaBorderRadius: Int = 12,
    isSICheckBoxChecked: Boolean = false,
    isSICheckBoxEnabled: Boolean = false,
    focusedTextInputBorderColor: String = "#2D2B32",
    unfocusedTextInputBorderColor: String = "#ADACB0",
    paymentMethodList: List<String> = emptyList(),
    isBoxPayProceedButtonVisible: Boolean,
    fontFamily: String? = null,
    handler: BoxPayElementsHandler?,
    onDismiss: () -> Unit
) = ComposeUIViewController {
    val paymentMethodTabs = paymentMethodList.mapNotNull { method ->
        runCatching { PaymentMethodTab.valueOf(method.uppercase()) }.getOrNull()
    }

    CommonSDKDismissHandler.setCloseSDK { onDismiss() }
    BoxPayCommonElements(
        paymentMethodList = paymentMethodTabs,
        token = token,
        isTestEnv = isTestEnv,
        shopperToken = shopperToken,
        showQROnLoad = showQROnLoad,
        ctaBorderRadius = ctaBorderRadius,
        isSICheckBoxChecked = isSICheckBoxChecked,
        isSICheckBoxEnabled = isSICheckBoxEnabled,
        focusedTextInputBorderColor = focusedTextInputBorderColor,
        unfocusedTextInputBorderColor = unfocusedTextInputBorderColor,
        isBoxPayProceedButtonVisible = isBoxPayProceedButtonVisible,
        fontFamily = fontFamily,
        handler = handler
    )
}