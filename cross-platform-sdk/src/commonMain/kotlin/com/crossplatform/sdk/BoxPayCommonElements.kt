package com.crossplatform.sdk

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.crossplatform.sdk.data.handler.BoxPayElementsHandler
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.di.appModule
import com.crossplatform.sdk.domain.model.PaymentMethodTab
import com.crossplatform.sdk.presentation.navigation.AppNavHost
import com.crossplatform.sdk.presentation.screens.BoxPayElementsComponent
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.koin.compose.KoinApplication

@Composable
fun BoxPayCommonElements(
    paymentMethodList : List<PaymentMethodTab>,
    token : String,
    isTestEnv : Boolean,
    shopperToken : String?,
    isBoxPayProceedButtonVisible : Boolean,
    ctaBorderRadius : Int,
    isSICheckBoxChecked : Boolean,
    isSICheckBoxEnabled : Boolean,
    focusedTextInputBorderColor : String,
    unfocusedTextInputBorderColor : String,
    showQROnLoad : Boolean,
    fontFamily : String?,
    handler: BoxPayElementsHandler? = null
) {
    KoinApplication(
        application = {
            modules(appModule)
        }
    ) {
        val backendFont by CheckoutDetailsHandler.fontFamilyFlow.collectAsStateWithLifecycle()

        ProvideSDKFonts(
            merchantFont = fontFamily,
            backendFont = backendFont,
        ) {
            CheckoutDetailsHandler.setCheckoutTokenForBoxPayElements(
                token = token,
                shopperToken = shopperToken,
                isTestEnv = isTestEnv,
                isSICheckboxEnabled = isSICheckBoxEnabled,
                isSICheckboxChecked = isSICheckBoxChecked,
                ctaBorderRadius = ctaBorderRadius,
                focusedTextInputBorderColor = focusedTextInputBorderColor,
                unfocusedTextInputBorderColor = unfocusedTextInputBorderColor,
                showQROnLoad = showQROnLoad
            )

            MaterialTheme {
                BoxPayElementsComponent(
                    paymentMethodList = paymentMethodList,
                    isBoxPayProceedButtonVisible = isBoxPayProceedButtonVisible,
                    shopperToken = shopperToken ?: "",
                    handler = handler
                )
            }
        }
    }
}