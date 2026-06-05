package com.application.androidkmpapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.application.androidkmpapp.ui.theme.CrossplatformsdkTheme
import com.crossplatform.BoxPayActivity
import com.crossplatform.sdk.data.handler.SDKPaymentResponseHandler
import com.crossplatform.sdk.data.model.SDKPaymentResponse
import com.crossplatform.sdk.presentation.theme.defaultFontFamily

class MainActivity : ComponentActivity() {
    private val paymentResult = mutableStateOf<SDKPaymentResponse?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrossplatformsdkTheme {
                Column(
                    modifier = Modifier.statusBarsPadding()
                ){
                    TokenScreen(
                        onProceed = { config ->
                            openBoxPayCheckout(config)
                        }
                    )
                }
            }

            paymentResult.value?.let { result ->
                AlertDialog(
                    onDismissRequest = { paymentResult.value = null },
                    title = {
                        Text(
                            text = "Payment Result",
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = defaultFontFamily,
                            fontSize = 18.sp
                        )
                    },
                    text = {
                        Text(
                            text = result.toString(),
                            fontFamily = defaultFontFamily,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { paymentResult.value = null }) {
                            Text("OK", fontFamily = defaultFontFamily)
                        }
                    }
                )
            }
        }
    }

    fun openBoxPayCheckout(config: BoxPayConfig) {
        SDKPaymentResponseHandler.set(::onPaymentResult)

        val intent = BoxPayActivity.createIntent(
            context                       = this,
            token                         = config.token,
            isTestEnv                     = config.isTestEnv,
            shopperToken                  = config.shopperToken,
            showQROnLoad                  = config.showQROnLoad,
            isSICheckBoxChecked           = config.isSICheckBoxChecked,
            isSICheckBoxEnabled           = config.isSICheckBoxEnabled,
            isFailedScreenVisible         = config.isFailedScreenVisible,
            isSuccessScreenVisible        = config.isSuccessScreenVisible,
            ctaBorderRadius               = config.ctaBorderRadius,
            focusedTextInputBorderColor   = config.focusedTextInputBorderColor,
            unfocusedTextInputBorderColor = config.unfocusedTextInputBorderColor
        )
        startActivity(intent)
    }

    fun onPaymentResult(result : SDKPaymentResponse) {
        paymentResult.value = result
    }
}