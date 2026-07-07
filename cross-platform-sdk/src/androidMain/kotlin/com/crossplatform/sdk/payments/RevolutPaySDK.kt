package com.crossplatform.sdk.payments

import androidx.activity.ComponentActivity
import com.crossplatform.sdk.domain.handler.ExpressCheckoutPaymentResult
import com.revolut.payments.RevolutPaymentsSDK
import com.revolut.revolutpay.api.PaymentResult
import com.revolut.revolutpay.api.RevolutPaymentController
import com.revolut.revolutpay.api.order.OrderParams
import com.revolut.revolutpay.api.revolutPay
import java.util.WeakHashMap
import androidx.core.net.toUri

object RevolutPaySdk {

    private val controllers = WeakHashMap<ComponentActivity, RevolutPaymentController>()
    private val pending = WeakHashMap<ComponentActivity, PendingState>()

    fun register(activity: ComponentActivity) {
        if (controllers.containsKey(activity)) return // idempotent -- safe to call more than once

        val state = PendingState()
        pending[activity] = state

        val controller = RevolutPaymentsSDK.revolutPay.createController(activity) { result ->
            val callback = state.callback
            val orderToken = state.orderToken
            state.callback = null
            state.orderToken = null

            val mapped = when (result) {
                is PaymentResult.Success ->
                    ExpressCheckoutPaymentResult.Success(orderToken.orEmpty())
                is PaymentResult.Failure -> {
                    val exception = result.exception
                    android.util.Log.e(
                        "RevolutPaySdk",
                        "Revolut Pay failed. class=${exception::class.qualifiedName} " +
                                "message=${exception.message} full=$exception",
                        exception
                    )
                    ExpressCheckoutPaymentResult.Failure(
                        exception.message ?: exception.toString()
                    )
                }
                is PaymentResult.UserAbandonedPayment ->
                    ExpressCheckoutPaymentResult.Cancelled
            }
            callback?.invoke(mapped)
        }

        controllers[activity] = controller
    }

    /** Used by AndroidPaymentHandler internally -- true only if register() was already called for this Activity. */
    internal fun isRegistered(activity: ComponentActivity): Boolean = controllers.containsKey(activity)

    /**
     * Used by AndroidPaymentHandler internally -- fails gracefully if
     * register() was never called.
     */
    internal fun launch(
        activity: ComponentActivity,
        orderToken: String,
        returnUrl: String,
        merchantPublicKey: String,
        isSandbox: Boolean,
        onResult: (ExpressCheckoutPaymentResult) -> Unit
    ) {
        val controller = controllers[activity]
        val state = pending[activity]
        if (controller == null || state == null) {
            onResult(
                ExpressCheckoutPaymentResult.Failure(
                    "Revolut Pay not registered for this Activity -- call " +
                            "RevolutPaySdk.register(this) in onCreate(), before setContent"
                )
            )
            return
        }

        // Confirmed mandatory by Revolut's own SDK at runtime:
        // "RevolutPaymentsSDK is not initialized, please call
        // RevolutPaymentsSDK.configure() before starting the payment process"
        RevolutPaymentsSDK.configure(
            RevolutPaymentsSDK.Configuration(
                merchantPublicKey = merchantPublicKey,
                environment = if (isSandbox) RevolutPaymentsSDK.Environment.SANDBOX else RevolutPaymentsSDK.Environment.PRODUCTION
            )
        )

        state.callback = onResult
        state.orderToken = orderToken

        controller.pay(
            OrderParams(
                orderToken = orderToken,
                returnUri = returnUrl.toUri(),
                requestShipping = false,
                savePaymentMethodForMerchant = false,
                customer = null
            )
        )
    }

    private class PendingState {
        var callback: ((ExpressCheckoutPaymentResult) -> Unit)? = null
        var orderToken: String? = null
    }
}