package com.crossplatform.sdk.payments

import androidx.activity.ComponentActivity
import com.affirm.android.Affirm
import com.affirm.android.model.Checkout
import com.affirm.android.model.Item
import com.crossplatform.sdk.domain.handler.AffirmCheckoutItem
import com.crossplatform.sdk.domain.handler.AffirmExpressCheckoutConfig
import com.crossplatform.sdk.domain.handler.ExpressCheckoutPaymentRequest
import com.crossplatform.sdk.domain.handler.ExpressCheckoutPaymentResult
import java.math.BigDecimal
import java.util.WeakHashMap

/**
 * Mirrors RevolutPaySDK.kt: an internal object that wraps Affirm's native
 * Android SDK behind a per-Activity registration. Unlike RevolutPay, Affirm
 * does not need a BoxPay order token up front -- the checkout object is
 * built from the cart on-device, and Affirm hands back a checkout token
 * once the shopper completes their flow. That token still has to be sent
 * to the BoxPay backend afterwards to authorize/capture the charge.
 *
 * IMPORTANT: the package names, class names and builder method signatures
 * below (Affirm, Checkout, Item, CheckoutCallbacks, setDisplayName/setSku/
 * setUnitPrice/setQty/setTotal/setShippingAmount/setTaxAmount, and whether
 * amounts are dollars or cents) were reconstructed from Affirm's public
 * docs (docs.affirm.com/payments/docs/checkout-android), NOT compiled
 * against the actual `com.affirm:affirm-android-sdk:2.0.34` artifact.
 * Confirm every signature against that SDK's javadoc/sources before
 * shipping -- these details do drift between SDK major versions.
 */
internal object AffirmSDK {
    
        private val callbackHosts =
                WeakHashMap<ComponentActivity, AffirmCallbackHost>()
    
        private var isConfigured = false
    
        /**
         * Called by BoxPayActivity before setContent(), same as
         * RevolutPaySDK.register(activity).
         */
        fun register(activity: ComponentActivity) {
                if (callbackHosts.containsKey(activity)) return
                callbackHosts[activity] = AffirmCallbackHost()
            }
    
        fun configure(
                publicKey: String,
                merchantName: String,
                isSandbox: Boolean
            ) {
                Affirm.initialize(
            Affirm.Configuration.Builder(publicKey)
                        .setEnvironment(if (isSandbox) Affirm.Environment.SANDBOX else Affirm.Environment.PRODUCTION)
                        .setMerchantName(merchantName)
                        .build()
                    )
                isConfigured = true
            }
    
        fun pay(
                activity: ComponentActivity,
                request: ExpressCheckoutPaymentRequest,
                config: AffirmExpressCheckoutConfig,
                callback: (ExpressCheckoutPaymentResult) -> Unit
            ) {
                val host = callbackHosts[activity]
                if (host == null) {
                        callback(ExpressCheckoutPaymentResult.Failure("Affirm not registered -- call AffirmSDK.register(activity) in Activity.onCreate() before setContent()"))
                        return
                    }
                if (!isConfigured) {
                        callback(ExpressCheckoutPaymentResult.Failure("Affirm not configured -- call AffirmSDK.configure(...) first"))
                        return
                    }
                if (config.items.isEmpty()) {
                        callback(ExpressCheckoutPaymentResult.Failure("Affirm checkout requires at least one line item"))
                        return
                    }
        
                host.callback = callback

                val itemsBySku: Map<String, Item> = config.items
                    .mapIndexed { index, item -> item.toAffirmItem("item-$index") }
                    .associateBy { it.sku() }

                val checkout = Checkout.builder()
                    .setItems(itemsBySku)
                    .setSendShippingAddresses(false) // no shipping address plumbed through yet
                    .setShippingAmount(BigDecimal.valueOf(config.shippingAmount))
                    .setTaxAmount(BigDecimal.valueOf(config.taxAmount))
                    .setTotal(BigDecimal.valueOf(request.amount))
                    .build()

                Affirm.startCheckout(activity, checkout, false)
            }
    
        /**
         * Must be called from BoxPayActivity.onActivityResult() -- Affirm's
         * checkout result comes back the classic way, not through the Activity
         * Result API contracts, so it can't be wired up with
         * rememberLauncherForActivityResult the way GooglePay is.
         */
        fun handleActivityResult(
                activity: ComponentActivity,
                requestCode: Int,
                resultCode: Int,
                data: android.content.Intent?
            ): Boolean {
                val host = callbackHosts[activity] ?: return false
                return Affirm.handleCheckoutData(host, requestCode, resultCode, data)
            }
    
        fun isAvailable(activity: ComponentActivity): Boolean =
                callbackHosts.containsKey(activity)
    
        private class AffirmCallbackHost : Affirm.CheckoutCallbacks {
                var callback: ((ExpressCheckoutPaymentResult) -> Unit)? = null
        
                override fun onAffirmCheckoutSuccess(token: String) {
                        val cb = callback
                        callback = null
                        cb?.invoke(ExpressCheckoutPaymentResult.Success(checkoutToken = token))
                    }
        
                override fun onAffirmCheckoutCancelled() {
                        val cb = callback
                        callback = null
                        cb?.invoke(ExpressCheckoutPaymentResult.Cancelled)
                    }
        
                override fun onAffirmCheckoutError(message: String?) {
                        val cb = callback
                        callback = null
                        cb?.invoke(ExpressCheckoutPaymentResult.Failure(message ?: "Affirm checkout failed"))
                    }
            }
    }

private fun AffirmCheckoutItem.toAffirmItem(fallbackSku: String): Item {
    val resolvedSku = sku.ifBlank { fallbackSku }
    return Item.builder()
        .setDisplayName(name)
        .setSku(resolvedSku)
        .setUnitPrice(BigDecimal.valueOf(unitPrice)) // dollars -- SDK converts to cents internally
        .setQty(quantity)
        .setUrl("") // required by Item.Builder; BoxPay doesn't carry a product page URL today
        .setImageUrl("") // required by Item.Builder; BoxPay doesn't carry a product image URL today
        .build()
}