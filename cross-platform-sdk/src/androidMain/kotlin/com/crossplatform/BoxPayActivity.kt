package com.crossplatform

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.bundle.Bundle
import com.crossplatform.sdk.BoxPayCommonCheckout
import com.crossplatform.sdk.data.handler.SDKPaymentResponseHandler

// New activity inside your SDK
class BoxPayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = intent.getStringExtra("token") ?: return finish()
        val env   = intent.getStringExtra("env") ?: "prod"

        setContent {
            BoxPayCommonCheckout(
                token = token,
                env = env,
                onPaymentResult = { result ->
                    SDKPaymentResponseHandler.notifyResult(result)  // ✅ forwards to merchant callback
                    finish()
                }
            )
        }
    }

    companion object {
        fun createIntent(context: Context, token: String, env: String) =
            Intent(context, BoxPayActivity::class.java).apply {
                putExtra("token", token)
                putExtra("env", env)
            }
    }
}