package com.crossplatform.sdk.data.handler

import com.crossplatform.sdk.data.model.SDKPaymentResponse

object SDKPaymentResponseHandler {
    // ─── Default (equivalent of onPaymentResult: () => {}) ────
    var onPaymentResult: (SDKPaymentResponse) -> Unit = {}

    // ─── setPaymentHandler ─────────────────────────────────────
    fun set(handler: (SDKPaymentResponse) -> Unit) {
        onPaymentResult = handler
    }

    // ─── Trigger the callback ──────────────────────────────────
    fun notifyResult(result: SDKPaymentResponse) {
        onPaymentResult(result)
    }
}