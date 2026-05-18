package com.crossplatform

import androidx.compose.ui.window.ComposeUIViewController
import com.crossplatform.sdk.BoxPayCommonCheckout
import com.crossplatform.sdk.data.model.SDKPaymentResponse
import platform.UIKit.UIViewController

// iosMain
fun BoxPayCommonCheckoutViewController(
    token: String,
    env: String,
    onPaymentResult: (SDKPaymentResponse) -> Unit
): UIViewController = ComposeUIViewController {
    BoxPayCommonCheckout(
        token = token,
        env = env,
        onPaymentResult = onPaymentResult
    )
}