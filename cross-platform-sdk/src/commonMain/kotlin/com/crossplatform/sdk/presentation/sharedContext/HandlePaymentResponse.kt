package com.crossplatform.sdk.presentation.sharedContext

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import com.crossplatform.sdk.presentation.getStatus
import com.crossplatform.sdk.presentation.resolveErrorMessage

fun handlePaymentResponse(
    response: ApiResponse<PaymentMethodPostResponse>,
    onSetPaymentUrl: ((String) -> Unit)? = null,
    onSetPaymentHtml: ((String) -> Unit)? = null,
    onNavigateToTimer: (() -> Unit)? = null,
    onOpenQr: ((String) -> Unit)? = null,
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

                    if(action != null) {
                        if(action.type == "html") {
                            onSetPaymentHtml?.invoke(action.htmlPageString ?: "")
                        }
                        else if (action.type == "redirect") {
                            onSetPaymentUrl?.invoke(action.url ?: "")
                        }
                        else if (action.type == "appRedirect") {
                            onOpenUpiIntent?.invoke(action.url ?: "")
                        }
                        else if (action.type == "qrCode") {
                            setIsBoxPayAnimationVisible(false)
                            onOpenQr?.invoke(action.content ?: "")
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
        else -> {
            CheckoutDetailsHandler.setErrorMessage(errorMessage)
            CheckoutDetailsHandler.setSessionFailed()
            setIsBoxPayAnimationVisible(false)
        }
    }
}