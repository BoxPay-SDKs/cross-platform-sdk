package com.crossplatform.sdk.data.handler

import com.crossplatform.sdk.data.model.SDKPaymentResponse

object CommonSDKDismissHandler {

    private var onClose: () -> Unit = {}

    fun setCloseSDK(close : () -> Unit) {
        onClose = close
    }

    fun notifyToCloseSDK() {
        onClose()
    }
}