package com.crossplatform

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.core.bundle.Bundle
import com.crossplatform.sdk.BoxPayCommonCheckout
import com.crossplatform.sdk.data.handler.CommonSDKDismissHandler
import com.crossplatform.sdk.payments.RevolutPaySdk

// New activity inside your SDK
class BoxPayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RevolutPaySdk.register(this)
        val token = intent.getStringExtra("token") ?: return finish()
        val env   = intent.getBooleanExtra("isTestEnv", false)
        val shopperToken = intent.getStringExtra("shopperToken")
        val isSuccessScreenVisible = intent.getBooleanExtra("isSuccessScreenVisible", false)
        val isFailedScreenVisible = intent.getBooleanExtra("isFailedScreenVisible", false)
        val showQROnLoad = intent.getBooleanExtra("showQROnLoad", false)
        val ctaBorderRadius = intent.getIntExtra("ctaBorderRadius", 12)
        val isSICheckBoxChecked = intent.getBooleanExtra("isSICheckBoxChecked", false)
        val isSICheckBoxEnabled = intent.getBooleanExtra("isSICheckBoxEnabled", false)
        val focusedTextInputBorderColor = intent.getStringExtra("focusedTextInputBorderColor") ?: ""
        val unfocusedTextInputBorderColor = intent.getStringExtra("unfocusedTextInputBorderColor") ?: ""
        val fontFamily = intent.getStringExtra("fontFamily")
        enableEdgeToEdge()

        CommonSDKDismissHandler.setCloseSDK { finish() }
        setContent {
            val insets = WindowInsets.systemBars
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(insets)
            ) {
                BoxPayCommonCheckout(
                    token = token,
                    isTestEnv = env,
                    shopperToken = shopperToken,
                    isSuccessScreenVisible = isSuccessScreenVisible,
                    isFailedScreenVisible = isFailedScreenVisible,
                    showQROnLoad = showQROnLoad,
                    ctaBorderRadius = ctaBorderRadius,
                    isSICheckBoxChecked = isSICheckBoxChecked,
                    isSICheckBoxEnabled = isSICheckBoxEnabled,
                    focusedTextInputBorderColor = focusedTextInputBorderColor,
                    unfocusedTextInputBorderColor = unfocusedTextInputBorderColor,
                    fontFamily = fontFamily
                )
            }
        }
    }

    companion object {
        fun createIntent(
            context: Context,
            token: String,
            isTestEnv: Boolean,
            shopperToken : String?,
            showQROnLoad : Boolean ,
            isSICheckBoxChecked : Boolean ,
            isSICheckBoxEnabled : Boolean ,
            isFailedScreenVisible : Boolean ,
            isSuccessScreenVisible : Boolean ,
            ctaBorderRadius : Int ,
            focusedTextInputBorderColor : String ,
            unfocusedTextInputBorderColor : String,
            fontFamily : String?
        ) =
            Intent(context, BoxPayActivity::class.java).apply {
                putExtra("token", token)
                putExtra("isTestEnv", isTestEnv)
                putExtra("shopperToken", shopperToken)
                putExtra("isSuccessScreenVisible", isSuccessScreenVisible)
                putExtra("isFailedScreenVisible", isFailedScreenVisible)
                putExtra("showQROnLoad", showQROnLoad)
                putExtra("isSICheckBoxChecked", isSICheckBoxChecked)
                putExtra("isSICheckBoxEnabled", isSICheckBoxEnabled)
                putExtra("ctaBorderRadius", ctaBorderRadius)
                putExtra("focusedTextInputBorderColor", focusedTextInputBorderColor)
                putExtra("unfocusedTextInputBorderColor", unfocusedTextInputBorderColor)
                putExtra("fontFamily", fontFamily)
            }
    }
}