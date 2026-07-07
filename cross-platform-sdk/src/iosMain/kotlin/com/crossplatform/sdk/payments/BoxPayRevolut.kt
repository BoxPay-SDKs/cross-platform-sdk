package com.crossplatform.sdk.payments

object BoxPayRevolut {
    private var gateway: RevolutPayGateway? = null

    fun configure(gateway: RevolutPayGateway) {
        this.gateway = gateway
    }

    fun pay(params: RevolutOrderParams, onResult: (RevolutPaymentOutcome) -> Unit) {
        val g = gateway
            ?: return onResult(RevolutPaymentOutcome.Failure("RevolutPayGateway not configured."))
        g.startPayment(params, onResult)
    }
}