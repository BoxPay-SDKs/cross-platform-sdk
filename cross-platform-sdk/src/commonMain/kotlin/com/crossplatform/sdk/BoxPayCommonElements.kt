package com.crossplatform.sdk

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.di.appModule
import com.crossplatform.sdk.presentation.navigation.AppNavHost
import com.crossplatform.sdk.presentation.screens.BoxPayElementsComponent
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.koin.compose.KoinApplication

@Composable
fun BoxPayCommonElements(
    paymentMethodList : List<String>,
    token : String,
    isTestEnv : Boolean,
    shopperToken : String?,
    isBoxPayProceedButtonVisible : Boolean,
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
            CheckoutDetailsHandler.setCheckoutTokenForBoxPayElements(
                token = token,
                shopperToken = shopperToken,
                isTestEnv = isTestEnv,
                isSICheckboxEnabled = isSICheckBoxEnabled,
                isSICheckboxChecked = isSICheckBoxChecked,
                ctaBorderRadius = ctaBorderRadius,
                focusedTextInputBorderColor = focusedTextInputBorderColor,
                unfocusedTextInputBorderColor = unfocusedTextInputBorderColor,
                paymentMethodList = paymentMethodList,
                isPayButtonVisible = isBoxPayProceedButtonVisible
            )

            MaterialTheme {
                BoxPayElementsComponent()
            }
        }
    }
}