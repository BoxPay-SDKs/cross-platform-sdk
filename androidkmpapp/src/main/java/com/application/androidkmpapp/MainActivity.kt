package com.application.androidkmpapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.application.androidkmpapp.ui.theme.CrossplatformsdkTheme
import com.crossplatform.sdk.BoxPayCommonCheckout

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val token = remember{ mutableStateOf("") }
            CrossplatformsdkTheme {
                Column(
                    modifier = Modifier.statusBarsPadding()
                ){
                    if(token.value.isNotEmpty()) {
                        BoxPayCommonCheckout(
                            token = token.value,
                            env = "test",
                            onPaymentResult = {
                                println("Payment Result: $it")
                                token.value = ""
                            }
                        )
                    } else {
                        TokenScreen(onChangeToken = {newToken ->
                            token.value = newToken
                        })
                    }
                }
            }
        }
    }
}