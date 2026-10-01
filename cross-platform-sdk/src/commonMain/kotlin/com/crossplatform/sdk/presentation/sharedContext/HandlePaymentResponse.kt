package com.crossplatform.sdk.presentation.sharedContext

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.parseErrorBodyAs
import com.crossplatform.sdk.domain.model.ApiErrorResponseModel
import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import com.crossplatform.sdk.presentation.getStatus
import com.crossplatform.sdk.presentation.resolveErrorMessage
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.payment_failed_error_messages
import org.jetbrains.compose.resources.getString

private const val PAYMENT_MAX_ATTEMPTS_REACHED = "BE_1815"
private const val CHECKOUT_MAX_ATTEMPTS_REACHED = "BE_1816"
internal suspend fun handlePaymentResponse(
    response: ApiResponse<PaymentMethodPostResponse>,
    onRevolutPay: ((String, String) -> Unit)? = null,
    onSetNativeOtp : ((Int, Int, String, String) -> Unit)? = null,
    onSetPaymentUrl: ((String) -> Unit)? = null,
    onSetPaymentHtml: ((String) -> Unit)? = null,
    onNavigateToTimer: (() -> Unit)? = null,
    onOpenQr: ((String, Int) -> Unit)? = null,
    onOpenUpiIntent: ((String) -> Unit)? = null,
    errorMessage : String,
    setIsBoxPayAnimationVisible : (Boolean) -> Unit
) {
    when(response) {
        is ApiResponse.Success -> {
            val apiData = response.data
            val status = getStatus(apiData.status.status)
            val transactionId = apiData.transactionId

            CheckoutDetailsHandler.setStatusAndTransID(
                status = status.name,
                transactionId = transactionId
            )

            when (status) {
                TransactionStatusEnum.REQUIRESACTION -> {
                    val action = apiData.actions?.firstOrNull()
                    val secondAction = apiData.actions?.getOrNull(1)

                    if(action != null) {
                        if(action.type == "html" && secondAction == null) {
                            onSetPaymentHtml?.invoke(action.htmlPageString ?: "")
                        }
                        else if (action.type == "redirect"  && secondAction == null) {
                            val method = action.method ?: "POST"
                            if (method.uppercase() == "POST" && !action.data.isNullOrEmpty()) {
                                // Build a self-submitting HTML form — exactly what React does
                                val fields = action.data.entries.joinToString("\n") { (k, v) ->
                                    """<input type="hidden" name="$k" value="$v"/>"""
                                }
                                val html = """
                                    <html><body onload="document.forms[0].submit()">
                                      <form method="POST" action="${action.url}">
                                        $fields
                                      </form>
                                    </body></html>
                                """.trimIndent()
                                onSetPaymentHtml?.invoke(html)    // reuse the existing html path
                            } else {
                                onSetPaymentUrl?.invoke(action.url ?: "")   // GET — safe as-is
                            }
                        }
                        else if (action.type == "appRedirect"  && secondAction == null) {
                            onOpenUpiIntent?.invoke(action.url ?: "")
                        }
                        else if (action.type == "qrCode" && secondAction == null) {
                            onOpenQr?.invoke(action.content ?: "", action.expirySec ?: 0)
                        }
                        else if (action.type == "info") {
                            onRevolutPay?.invoke(action.token ?: "", secondAction?.url ?: "")
                        }
                        else if (secondAction?.type == "nativeOtp") {
                            onSetNativeOtp?.invoke(
                                secondAction.minLength ?: 6,
                                secondAction.maxLength ?: 6,
                                action.url ?: "",
                                action.htmlPageString ?: ""
                            )
                        }
                        else {
                            setIsBoxPayAnimationVisible(false)
                            onNavigateToTimer?.invoke()
                        }
                    }
                    else {
                        setIsBoxPayAnimationVisible(false)
                        onNavigateToTimer?.invoke()
                    }
                }

                TransactionStatusEnum.FAILED -> {
                    val resolvedErrorMessage = resolveErrorMessage(
                        reasonCode = apiData.status.reasonCode,
                        reason = apiData.status.reason,
                        fallback = errorMessage
                    )

                    CheckoutDetailsHandler.setErrorMessage(resolvedErrorMessage)
                    CheckoutDetailsHandler.setSessionFailed()
                    setIsBoxPayAnimationVisible(false)
                }

                TransactionStatusEnum.SUCCESS -> {
                    CheckoutDetailsHandler.setTimeAndPaymentMethod(
                        timeStamp = response.data.transactionTimestampLocale,
                        paymentMethod = response.data.paymentMethod.brand ?: ""
                    )
                    CheckoutDetailsHandler.setSessionSuccess()
                    setIsBoxPayAnimationVisible(false)
                }

                TransactionStatusEnum.EXPIRED  -> {
                    CheckoutDetailsHandler.setSessionExpired()
                    setIsBoxPayAnimationVisible(false)
                }

                else -> {
                    CheckoutDetailsHandler.setErrorMessage(errorMessage)
                    CheckoutDetailsHandler.setSessionFailed()
                    setIsBoxPayAnimationVisible(false)
                }
            }
        }

        is ApiResponse.Error -> {
            val structuredError = response.parseErrorBodyAs<ApiErrorResponseModel>()

            when {
                structuredError?.reasonCode == PAYMENT_MAX_ATTEMPTS_REACHED -> {
//                    val ignoredMethods = setOf("Upi", "UpiOneTimeMandate")

                    val availableMethods = structuredError.fieldErrorItems
//                        .filter { it.fieldName !in ignoredMethods }
                        .filter { (it.message?.toIntOrNull() ?: 0) > 0 }
                        .map { it.fieldName }

                    CheckoutDetailsHandler.setRetryAvailableMethods(availableMethods)
                    CheckoutDetailsHandler.setIsPaymentMaxAttemptsReached()
                    setIsBoxPayAnimationVisible(false)
                }

                structuredError?.reasonCode == CHECKOUT_MAX_ATTEMPTS_REACHED -> {
                    CheckoutDetailsHandler.setIsCheckoutMaxAttemptsReached()
                    setIsBoxPayAnimationVisible(false)
                }

                errorMessage.contains("expired", true) -> {
                    CheckoutDetailsHandler.setSessionExpired()
                    setIsBoxPayAnimationVisible(false)
                }

                else -> {
                    val resolvedErrorMessage = resolveErrorMessage(
                        reasonCode = structuredError?.reasonCode,
                        reason = structuredError?.message,
                        fallback = getString(Res.string.payment_failed_error_messages)
                    )
                    CheckoutDetailsHandler.setErrorMessage(
                        resolvedErrorMessage
                    )
                    CheckoutDetailsHandler.setSessionFailed()
                    setIsBoxPayAnimationVisible(false)
                }
            }
        }
        else -> {
            // no operations
        }
    }
}