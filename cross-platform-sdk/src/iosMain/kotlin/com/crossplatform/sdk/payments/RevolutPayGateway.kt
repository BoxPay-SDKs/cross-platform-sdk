package com.crossplatform.sdk.payments

interface RevolutPayGateway {
    fun startPayment(params: RevolutOrderParams, onResult: (RevolutPaymentOutcome) -> Unit)
}

data class RevolutOrderParams(
    val orderToken: String,
    val requestShipping: Boolean = false,
    val savePaymentMethodForMerchant: Boolean = false
)

sealed class RevolutPaymentOutcome {
    data object Success : RevolutPaymentOutcome()
    data class Failure(val reason: String) : RevolutPaymentOutcome()
    data object UserAbandoned : RevolutPaymentOutcome()

    val isSuccess: Boolean get() = this is Success
    val isUserAbandoned: Boolean get() = this is UserAbandoned
    val failureReason: String? get() = (this as? Failure)?.reason

    companion object {
        fun success(): RevolutPaymentOutcome = Success
        fun failure(reason: String): RevolutPaymentOutcome = Failure(reason)
        fun userAbandoned(): RevolutPaymentOutcome = UserAbandoned
    }
}