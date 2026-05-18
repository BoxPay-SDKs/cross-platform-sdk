package com.crossplatform.sdk

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.handler.SDKPaymentResponseHandler
import com.crossplatform.sdk.data.model.SDKPaymentResponse
import com.crossplatform.sdk.di.appModule
import com.crossplatform.sdk.presentation.navigation.AppNavHost
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.koin.compose.KoinApplication

@Composable
fun BoxPayCommonCheckout(
    token : String,
    env : String,
    shopperToken : String? = null,
    onPaymentResult: (SDKPaymentResponse) -> Unit
) {
    KoinApplication(
        application = {
            modules(appModule)
        }
    ) {
        ProvideSDKFonts {
            CheckoutDetailsHandler.setCheckoutToken(
                token = token,
                shopperToken = shopperToken,
                env = env,
                isSICheckboxVisible = false,
                ctaBorderRadius = 12,
                isSuccessScreenVisible = true
            )

            SDKPaymentResponseHandler.set(onPaymentResult)

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost()
                }
            }
        }
    }
}