package com.application.androidkmpapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.application.androidkmpapp.ui.theme.CrossplatformsdkTheme
import com.crossplatform.BoxPayActivity
import com.crossplatform.sdk.BoxPayCommonElements
import com.crossplatform.sdk.data.handler.BoxPayElementsHandler
import com.crossplatform.sdk.data.handler.SDKPaymentResponseHandler
import com.crossplatform.sdk.data.model.SDKPaymentResponse
import com.crossplatform.sdk.domain.model.PaymentMethodTab
import com.crossplatform.sdk.presentation.theme.LocalSDKFonts

class MainActivity : ComponentActivity() {
    private val paymentResult = mutableStateOf<SDKPaymentResponse?>(null)

    // null = show TokenScreen; non-null = show inline Elements with our own button
    private val elementsConfig = mutableStateOf<BoxPayConfig?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrossplatformsdkTheme {
                Column(modifier = Modifier.statusBarsPadding()) {
                    val config = elementsConfig.value
                    if (config == null) {
                        TokenScreen(
                            onProceed = { cfg ->
                                if (cfg.paymentMethodList.isEmpty()) {
                                    openBoxPayCheckout(cfg)
                                } else {
                                    // Elements + merchant button → render inline
                                    SDKPaymentResponseHandler.set(::onPaymentResult)
                                    elementsConfig.value = cfg
                                }
                            }
                        )
                    } else {
                        val handler = remember { BoxPayElementsHandler() }
                        val isPayable by handler.isPayable.collectAsStateWithLifecycle()

                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                                BoxPayCommonElements(
                                    paymentMethodList = config.paymentMethodList.mapNotNull {
                                        runCatching { PaymentMethodTab.valueOf(it.uppercase()) }.getOrNull()
                                    },
                                    token = config.token,
                                    isTestEnv = config.isTestEnv,
                                    isBoxPayProceedButtonVisible = config.isBoxPayPayButtonVisible,
                                    shopperToken = config.shopperToken,
                                    showQROnLoad = config.showQROnLoad,
                                    ctaBorderRadius = config.ctaBorderRadius,
                                    isSICheckBoxChecked = config.isSICheckBoxChecked,
                                    isSICheckBoxEnabled = config.isSICheckBoxEnabled,
                                    focusedTextInputBorderColor = config.focusedTextInputBorderColor,
                                    unfocusedTextInputBorderColor = config.unfocusedTextInputBorderColor,
                                    fontFamily = config.fontFamily,
                                    handler = handler,
                                )
                            }

                            if(!config.isBoxPayPayButtonVisible) {
                                Button(
                                    onClick = { handler.pay() },
                                    enabled = isPayable,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                                ) {
                                    Text(
                                        text = if (isPayable) "Pay Now" else "Select a payment method",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            paymentResult.value?.let { result ->
                AlertDialog(
                    onDismissRequest = { paymentResult.value = null },
                    title = {
                        Text(
                            text = "Payment Result",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Text(
                            text = result.toString(),
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { paymentResult.value = null }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }

    fun openBoxPayCheckout(config: BoxPayConfig) {
        SDKPaymentResponseHandler.set(::onPaymentResult)
        val intent = BoxPayActivity.createIntent(
            context = this,
            token = config.token,
            isTestEnv = config.isTestEnv,
            shopperToken = config.shopperToken,
            showQROnLoad = config.showQROnLoad,
            isSICheckBoxChecked = config.isSICheckBoxChecked,
            isSICheckBoxEnabled = config.isSICheckBoxEnabled,
            isFailedScreenVisible = config.isFailedScreenVisible,
            isSuccessScreenVisible = config.isSuccessScreenVisible,
            ctaBorderRadius = config.ctaBorderRadius,
            focusedTextInputBorderColor = config.focusedTextInputBorderColor,
            unfocusedTextInputBorderColor = config.unfocusedTextInputBorderColor,
            fontFamily = config.fontFamily
        )
        startActivity(intent)
    }

    fun onPaymentResult(result: SDKPaymentResponse) {
        paymentResult.value = result
        elementsConfig.value = null   // leave the Elements screen on result
    }
}