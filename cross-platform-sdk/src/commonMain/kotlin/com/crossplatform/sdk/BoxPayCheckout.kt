package com.crossplatform.sdk

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.di.appModule
import com.crossplatform.sdk.presentation.navigation.AppNavHost
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.koin.compose.KoinApplication

@Composable
fun BoxPayCommonCheckout(
    token : String,
    isTestEnv : Boolean,
    shopperToken : String?,
    isSuccessScreenVisible : Boolean,
    isFailedScreenVisible : Boolean,
    showQROnLoad : Boolean,
    ctaBorderRadius : Int,
    isSICheckBoxChecked : Boolean,
    isSICheckBoxEnabled : Boolean,
    focusedTextInputBorderColor : String,
    unfocusedTextInputBorderColor : String
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
                isTestEnv = isTestEnv,
                isSICheckboxEnabled = isSICheckBoxEnabled,
                isSICheckboxChecked = isSICheckBoxChecked,
                ctaBorderRadius = ctaBorderRadius,
                isSuccessScreenVisible = isSuccessScreenVisible,
                isFailedScreenVisible = isFailedScreenVisible,
                focusedTextInputBorderColor = focusedTextInputBorderColor,
                unfocusedTextInputBorderColor = unfocusedTextInputBorderColor,
                showQROnLoad = showQROnLoad
            )

            MaterialTheme {
                AppNavHost()
            }
        }
    }
}