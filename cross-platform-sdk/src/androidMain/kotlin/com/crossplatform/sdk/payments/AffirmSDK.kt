package com.crossplatform.sdk.payments

import androidx.activity.ComponentActivity
import com.affirm.android.Affirm
import com.affirm.android.model.Address
import com.affirm.android.model.Billing
import com.affirm.android.model.Checkout
import com.affirm.android.model.Item
import com.affirm.android.model.Name
import com.affirm.android.model.Shipping
import com.crossplatform.sdk.domain.handler.AffirmCheckoutItem
import com.crossplatform.sdk.domain.handler.AffirmExpressCheckoutConfig
import com.crossplatform.sdk.domain.handler.ExpressCheckoutPaymentRequest
import com.crossplatform.sdk.domain.handler.ExpressCheckoutPaymentResult
import java.math.BigDecimal
import java.util.WeakHashMap

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
                .setLogLevel(Affirm.LOG_LEVEL_DEBUG)
                .setCheckoutRequestCode(8001)
                .setVcnCheckoutRequestCode(8002)
                .setPrequalRequestCode(8003)
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
        val shippingAddress = config.shippingAddress

        host.callback = callback

        val itemsBySku: Map<String, Item> = config.items
            .mapIndexed { index, item -> item.toAffirmItem(fallbackSku = "item-$index") }
            .associateBy { it.sku() }

        val affirmAddress = Address.builder()
            .setStreet1(shippingAddress.street1)
            .apply { shippingAddress.street2?.takeIf { it.isNotBlank() }?.let { setStreet2(it) } }
            .setCity(shippingAddress.city)
            .setRegion1Code(shippingAddress.regionCode)
            .setPostalCode(shippingAddress.postalCode)
            .setCountry(shippingAddress.country)
            .build()

        val affirmName = Name.builder()
            .setFull(shippingAddress.fullName)
            .build()

        val shipping = Shipping.builder()
            .setAddress(affirmAddress)
            .setName(affirmName)
            .build()

        val billing = Billing.builder()
            .setAddress(affirmAddress)
            .setName(affirmName)
            .build()

        val checkout = Checkout.builder()
            .setOrderId(config.orderId.ifBlank { null })
            .setItems(itemsBySku)
            .setShipping(shipping)
            .setBilling(billing)
            .setShippingAmount(BigDecimal.valueOf(config.shippingAmount))
            .setTaxAmount(BigDecimal.valueOf(config.taxAmount))
            .setTotal(BigDecimal.valueOf(request.amount))
            .build()

        Affirm.startCheckout(activity, checkout, false)
    }

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
        .build()
}