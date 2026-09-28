package com.crossplatform.sdk.domain.handler

import com.crossplatform.sdk.data.model.AllowedPaymentMethods

// commonMain
internal interface ExpressCheckoutPaymentHandler {
    suspend fun isGooglePayAvailable(config: GooglePayExpressCheckoutConfig): Boolean
    fun isApplePayAvailable(): Boolean
    fun isRevolutPayAvailable(): Boolean
    fun isAffirmAvailable(): Boolean
    fun launchGooglePay(request: ExpressCheckoutPaymentRequest, config: GooglePayExpressCheckoutConfig, onResult: (ExpressCheckoutPaymentResult) -> Unit)
    fun launchApplePay(request: ExpressCheckoutPaymentRequest, config: ApplePayExpressCheckoutConfig, onResult: (ExpressCheckoutPaymentResult) -> Unit)
    fun launchRevolutPay(request: ExpressCheckoutPaymentRequest, config: RevolutPayExpressCheckoutConfig, isSandbox : Boolean, onResult: (ExpressCheckoutPaymentResult) -> Unit)
    fun launchAffirm(request: ExpressCheckoutPaymentRequest, config: AffirmExpressCheckoutConfig, isSandbox : Boolean, onResult: (ExpressCheckoutPaymentResult) -> Unit)
}

internal data class ExpressCheckoutPaymentRequest(
    val amount: Double,
    val currencyCode: String,
    val countryCode: String
)

internal sealed class ExpressCheckoutPaymentResult {
    data class Success(val googleToken : String? = null, val checkoutToken : String? = null) : ExpressCheckoutPaymentResult()
    data class Failure(val message: String) : ExpressCheckoutPaymentResult()
    data object Cancelled : ExpressCheckoutPaymentResult()
}

internal data class RevolutPayExpressCheckoutConfig(
    val revolutReturnUrl: String,
    val orderToken : String,
    val merchantPublicKey : String
)

// Affirm builds its own checkout object on-device (it never receives an
// order token from BoxPay first, unlike RevolutPay) — it needs the public
// API key plus an itemised cart, then hands back a checkout token that the
// BoxPay backend must confirm/authorize server-side using Affirm's private
// key (same shape as how GooglePay's token is sent to initiatePayment).
internal data class AffirmExpressCheckoutConfig(
    val publicKey: String,
    val orderId : String,
    val merchantName: String,
    val items: List<AffirmCheckoutItem>,
    val shippingAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val shippingAddress: AffirmAddress
)

internal data class AffirmCheckoutItem(
    val name: String,
    val sku: String,
    val unitPrice: Double,
    val quantity: Int
)

internal data class AffirmAddress(
    val fullName: String,
    val street1: String,
    val street2: String? = null,
    val city: String,
    val regionCode: String,
    val postalCode: String,
    val country: String = "USA"
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
