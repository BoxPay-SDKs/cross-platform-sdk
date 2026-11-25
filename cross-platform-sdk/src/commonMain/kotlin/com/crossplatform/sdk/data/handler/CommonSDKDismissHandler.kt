package com.crossplatform.sdk.data.handler

import com.crossplatform.sdk.data.ServiceRequest

internal object CommonSDKDismissHandler {

    private var onClose: () -> Unit = {}

    fun setCloseSDK(close : () -> Unit) {
        onClose = close
    }

    fun notifyToCloseSDK() {
        onClose()
    }

    fun releaseSession() {
        SDKJobHandler.cancelAll()
        ServiceRequest.close()
        CheckoutDetailsHandler.resetToDefault()
        UserDataHandler.resetToDefault()
    }
}