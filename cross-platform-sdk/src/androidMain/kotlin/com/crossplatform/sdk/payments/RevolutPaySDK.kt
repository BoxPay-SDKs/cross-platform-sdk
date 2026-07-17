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

object RevolutPaySDK {

    private val controllers =
        WeakHashMap<ComponentActivity, RevolutPaymentController>()

    private val states =
        WeakHashMap<ComponentActivity, State>()


    /**
     * Called by host Activity before setContent()
     */
    fun register(activity: ComponentActivity) {

        if (controllers.containsKey(activity)) return


        val state = State()
        states[activity] = state


        val controller =
            RevolutPaymentsSDK.revolutPay.createController(activity) { result ->

                val callback = state.callback
                val token = state.orderToken


                state.callback = null
                state.orderToken = null


                when(result) {

                    is PaymentResult.Success -> {
                        callback?.invoke(
                            ExpressCheckoutPaymentResult.Success
                        )
                    }


                    is PaymentResult.UserAbandonedPayment -> {
                        callback?.invoke(
                            ExpressCheckoutPaymentResult.Cancelled
                        )
                    }


                    is PaymentResult.Failure -> {
                        callback?.invoke(
                            ExpressCheckoutPaymentResult.Failure(
                                result.exception.message
                                    ?: result.exception.toString()
                            )
                        )
                    }
                }
            }


        controllers[activity] = controller
    }


    fun configure(
        merchantPublicKey: String,
        isSandbox: Boolean
    ) {

        RevolutPaymentsSDK.configure(
            RevolutPaymentsSDK.Configuration(
                merchantPublicKey = merchantPublicKey,
                environment = RevolutPaymentsSDK.Environment.PRODUCTION
            )
        )
    }



    fun pay(
        activity: ComponentActivity,
        orderToken: String,
        returnUrl: String,
        callback: (ExpressCheckoutPaymentResult) -> Unit
    ) {

        val controller = controllers[activity]
        val state = states[activity]


        if(controller == null || state == null) {
            callback(
                ExpressCheckoutPaymentResult.Failure(
                    "RevolutPay not registered"
                )
            )
            return
        }


        state.callback = callback
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


    fun isAvailable(activity: ComponentActivity): Boolean {
        return controllers.containsKey(activity)
    }


    private class State {
        var callback:
                ((ExpressCheckoutPaymentResult) -> Unit)? = null

        var orderToken: String? = null
    }
}