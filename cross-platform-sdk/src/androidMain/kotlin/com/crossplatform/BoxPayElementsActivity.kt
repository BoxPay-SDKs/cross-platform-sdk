package com.crossplatform

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.crossplatform.sdk.BoxPayCommonElements
import com.crossplatform.sdk.data.handler.BoxPayElementsHandler
import com.crossplatform.sdk.domain.model.PaymentMethodTab

object BoxPayElementsView {

    @JvmStatic
    @JvmOverloads
    fun create(
        context: Context,
        handler: BoxPayElementsHandler,
        token: String,
        isTestEnv: Boolean,
        shopperToken: String? = null,
        showQROnLoad: Boolean = false,
        isSICheckBoxChecked: Boolean = false,
        isSICheckBoxEnabled: Boolean = false,
        ctaBorderRadius: Int = 12,
        focusedTextInputBorderColor: String = "#2D2B32",
        unfocusedTextInputBorderColor: String = "#ADACB0",
        paymentMethodList: List<String> = emptyList(),
        fontFamily: String? = null,
    ): View {
        val paymentMethodTabs = paymentMethodList.mapNotNull { method ->
            runCatching { PaymentMethodTab.valueOf(method.uppercase()) }.getOrNull()
        }

        return ComposeView(context).apply {
            // merchant controls their own screen; if they need a close hook, expose it.
            setContent {
                Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                    BoxPayCommonElements(
                        paymentMethodList = paymentMethodTabs,
                        token = token,
                        isTestEnv = isTestEnv,
                        // merchant owns the button → hide the SDK's internal one
                        isBoxPayProceedButtonVisible = false,
                        shopperToken = shopperToken,
                        showQROnLoad = showQROnLoad,
                        ctaBorderRadius = ctaBorderRadius,
                        isSICheckBoxChecked = isSICheckBoxChecked,
                        isSICheckBoxEnabled = isSICheckBoxEnabled,
                        focusedTextInputBorderColor = focusedTextInputBorderColor,
                        unfocusedTextInputBorderColor = unfocusedTextInputBorderColor,
                        fontFamily = fontFamily,
                        handler = handler,        // SDK publishes validity + submit
                    )
                }
            }
        }
    }
}