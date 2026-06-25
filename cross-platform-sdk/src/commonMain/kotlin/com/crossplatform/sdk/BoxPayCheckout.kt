package com.crossplatform.sdk

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    unfocusedTextInputBorderColor : String,
    fontFamily: String?
) {
    KoinApplication(
        application = {
            modules(appModule)
        }
    ) {
        val backendFont by CheckoutDetailsHandler.fontFamilyFlow.collectAsStateWithLifecycle()

        ProvideSDKFonts(
            merchantFont = fontFamily,   // priority 1
            backendFont = backendFont,   // priority 2 (default is handled inside)
            onUnknownFontRequested = { name ->
                // will not be implemented for now
            }
        ) {
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