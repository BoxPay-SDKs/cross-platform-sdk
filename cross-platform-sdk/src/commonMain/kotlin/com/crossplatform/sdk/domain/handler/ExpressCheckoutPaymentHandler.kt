package com.crossplatform.sdk.domain.handler

// commonMain
interface ExpressCheckoutPaymentHandler {
    fun isGooglePayAvailable(): Boolean
    fun isApplePayAvailable(): Boolean
    fun isRevolutPayAvailable(): Boolean
    fun launchGooglePay(request: ExpressCheckoutPaymentRequest, config: ExpressCheckoutConfig, onResult: (ExpressCheckoutPaymentResult) -> Unit)
    fun launchApplePay(request: ExpressCheckoutPaymentRequest, config: ExpressCheckoutConfig, onResult: (ExpressCheckoutPaymentResult) -> Unit)
    fun launchRevolutPay(request: ExpressCheckoutPaymentRequest, config: ExpressCheckoutConfig,merchantPublicKey : String, isSandbox : Boolean, onResult: (ExpressCheckoutPaymentResult) -> Unit)
}

data class ExpressCheckoutPaymentRequest(
    val amount: String,
    val currencyCode: String,
    val merchantName: String,
    val countryCode: String,
    val orderToken: String? = null
)

sealed class ExpressCheckoutPaymentResult {
    data class Success(val token: String) : ExpressCheckoutPaymentResult()
    data class Failure(val message: String) : ExpressCheckoutPaymentResult()
    data object Cancelled : ExpressCheckoutPaymentResult()
}

data class ExpressCheckoutConfig(
    val applePayMerchantIdentifier: String,
    val googlePayGateway: String,
    val googlePayGatewayMerchantId: String,
    val revolutReturnUrl: String
)
