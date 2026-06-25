package com.crossplatform

import androidx.compose.ui.window.ComposeUIViewController
import com.crossplatform.sdk.BoxPayCommonElements
import com.crossplatform.sdk.data.handler.BoxPayElementsHandler
import com.crossplatform.sdk.data.handler.CommonSDKDismissHandler
import com.crossplatform.sdk.domain.model.PaymentMethodTab

fun BoxPayElementsViewController(
    token : String,
    isTestEnv: Boolean,
    shopperToken: String?,
    showQROnLoad: Boolean,
    ctaBorderRadius: Int,
    isSICheckBoxChecked: Boolean,
    isSICheckBoxEnabled: Boolean,
    focusedTextInputBorderColor: String,
    unfocusedTextInputBorderColor: String,
    onDismiss: () -> Unit,
    paymentMethodList : List<PaymentMethodTab>,
    isBoxPayPayButtonVisible : Boolean,
    fontFamily : String?,
    handler: BoxPayElementsHandler?
) = ComposeUIViewController {
    CommonSDKDismissHandler.setCloseSDK { onDismiss() }
    BoxPayCommonElements(
        paymentMethodList = paymentMethodList,
        token = token,
        isTestEnv = isTestEnv,
        shopperToken = shopperToken,
        showQROnLoad = showQROnLoad,
        ctaBorderRadius = ctaBorderRadius,
        isSICheckBoxChecked = isSICheckBoxChecked,
        isSICheckBoxEnabled = isSICheckBoxEnabled,
        focusedTextInputBorderColor = focusedTextInputBorderColor,
        unfocusedTextInputBorderColor = unfocusedTextInputBorderColor,
        isBoxPayProceedButtonVisible = isBoxPayPayButtonVisible,
        fontFamily = fontFamily,
        handler = handler
    )
}