package com.crossplatform.sdk.payments

fun interface RevolutPayExecutor {
    fun launch(
        orderToken: String,
        merchantPublicKey: String,
        isSandbox: Boolean,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    )
}

object RevolutPayBridgeRegistry {
    var executor: RevolutPayExecutor? = null
}