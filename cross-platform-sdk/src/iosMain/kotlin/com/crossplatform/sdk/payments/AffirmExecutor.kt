package com.crossplatform.sdk.payments

/**
 * Mirrors RevolutPayExecutor.kt: this KMM module does not vendor Affirm's
 * iOS SDK (no `pod 'AffirmSDK'` dependency in cross_platform_sdk.podspec),
 * so the host iOS app must integrate Affirm's SDK itself, implement this
 * bridge in Swift, and register it before the checkout screen is shown --
 * exactly like RevolutPayExecutor / RevolutPayBridgeRegistry today.
 *
 * Swift-side sketch (host app):
 *
 *   import AffirmSDK
 *   import cross_platform_sdk
 *
 *   class AffirmBridge: AffirmExecutor {
 *       func launch(publicKey: String, merchantName: String, items: [AffirmCheckoutItem],
 *                    shippingAmount: Double, taxAmount: Double, totalAmount: Double,
 *                    isSandbox: Bool, onResult: @escaping (Bool, String?, String?) -> Void) {
 *           AffirmConfiguration.sharedInstance().configure(
 *               withPublicKey: publicKey,
 *               environment: isSandbox ? .sandbox : .production,
 *               merchantName: merchantName
 *           )
 *           let checkout = AffirmCheckout(...)  // build from items/shippingAmount/taxAmount/totalAmount
 *           AffirmCheckoutViewController.startCheckout(checkout, useVCN: false, getReasonCodes: false, delegate: self)
 *           // in the AffirmCheckoutDelegate callbacks, call onResult(success, checkoutToken, errorMessage)
 *       }
 *   }
 *
 *   AffirmBridgeRegistry.executor = AffirmBridge()
 */
fun interface AffirmExecutor {
    fun launch(
        publicKey: String,
        merchantName: String,
        items: List<AffirmExecutorItem>,
        shippingAmount: Double,
        taxAmount: Double,
        totalAmount: Double,
        isSandbox: Boolean,
        onResult: (success: Boolean, checkoutToken: String?, errorMessage: String?) -> Unit
    )
}

data class AffirmExecutorItem(
    val name: String,
    val sku: String,
    val unitPrice: Double,
    val quantity: Int
)

object AffirmBridgeRegistry {
    var executor: AffirmExecutor? = null
}