package com.crossplatform.sdk.domain.handler

import com.crossplatform.sdk.data.model.AllowedPaymentMethods

// commonMain
internal interface ExpressCheckoutPaymentHandler {
    suspend fun isGooglePayAvailable(config: GooglePayExpressCheckoutConfig): Boolean
    fun isApplePayAvailable(): Boolean
    fun isRevolutPayAvailable(): Boolean
    fun launchGooglePay(request: ExpressCheckoutPaymentRequest, config: GooglePayExpressCheckoutConfig, onResult: (ExpressCheckoutPaymentResult) -> Unit)
    fun launchApplePay(request: ExpressCheckoutPaymentRequest, config: ApplePayExpressCheckoutConfig, onResult: (ExpressCheckoutPaymentResult) -> Unit)
    fun launchRevolutPay(request: ExpressCheckoutPaymentRequest, config: RevolutPayExpressCheckoutConfig, isSandbox : Boolean, onResult: (ExpressCheckoutPaymentResult) -> Unit)
}

internal data class ExpressCheckoutPaymentRequest(
    val amount: String,
    val currencyCode: String,
    val countryCode: String
)

internal sealed class ExpressCheckoutPaymentResult {
    data class Success(val googleToken : String? = null) : ExpressCheckoutPaymentResult()
    data class Failure(val message: String) : ExpressCheckoutPaymentResult()
    data object Cancelled : ExpressCheckoutPaymentResult()
}

internal data class RevolutPayExpressCheckoutConfig(
    val revolutReturnUrl: String,
    val orderToken : String,
    val merchantPublicKey : String
)

internal data class GooglePayExpressCheckoutConfig(
    val gateway: String,
    val merchantId: String,
    val merchantName : String,
    val allowedPaymentMethods : List<AllowedPaymentMethods>,
    val siteReference : String
)

internal data class ApplePayExpressCheckoutConfig(
    val gateway : String,
    val merchantName : String,
    val siteReference : String,
    val merchantCapabilities : List<String>,
    val supportedNetworks : List<String>
)
