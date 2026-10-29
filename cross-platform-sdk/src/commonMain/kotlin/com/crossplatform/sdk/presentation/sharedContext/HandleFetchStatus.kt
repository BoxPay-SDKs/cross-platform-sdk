package com.crossplatform.sdk.presentation.sharedContext

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import com.crossplatform.sdk.presentation.getFetchStatus
import com.crossplatform.sdk.presentation.resolveErrorMessage
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.payment_failed_error_messages
import org.jetbrains.compose.resources.getString

/**
 * Resolves the shared "payment failed" fallback string, defensively.
 *
 * [getString] depends on the platform's Compose-resources Android context
 * being initialized. That's always true in a running app, but in a JVM/
 * Robolectric unit test it can be unset depending on test ordering/startup —
 * in that situation [getString] throws rather than returning a value. Since
 * this is only ever used as a last-resort fallback message (real reason/
 * reasonCode strings from the API take precedence — see [resolveErrorMessage]),
 * falling back further to a plain literal here is safe and never changes
 * behavior when the resource loads normally.
 */
internal suspend fun paymentFailedFallbackMessage(): String =
    try {
        getString(Res.string.payment_failed_error_messages)
    } catch (e: Exception) {
        "Payment failed. Please try again."
    }

internal suspend fun handleFetchStatus(
    response : ApiResponse<FetchStatusResponse>,
    setIsBoxPayAnimationVisible : (Boolean) -> Unit,
    onAutoRetry : () -> Unit,
    notifyComponentsHandler : (() -> Unit)? = null
) {
    when(response) {
        is ApiResponse.Error -> {
            CheckoutDetailsHandler.setErrorMessage()
            CheckoutDetailsHandler.setSessionFailed()
            setIsBoxPayAnimationVisible(false)
        }
        is ApiResponse.Success -> {
            val apiData = response.data
            val status = getFetchStatus(apiData.status)
            val transactionId = apiData.transactionId

            CheckoutDetailsHandler.setStatusAndTransID(
                status =  status.name,
                transactionId = transactionId
            )

            when (status){
                TransactionStatusEnum.SUCCESS -> {
                    CheckoutDetailsHandler.setTimeAndPaymentMethod(
                        timeStamp = response.data.transactionTimestampLocale,
                        paymentMethod = response.data.paymentMethod.brand ?: ""
                    )
                    CheckoutDetailsHandler.setSessionSuccess()
                    setIsBoxPayAnimationVisible(false)
                }
                TransactionStatusEnum.FAILED -> {
                    if(response.data.retryable) {
                        onAutoRetry()
                        return
                    }
                    val resolvedErrorMessage = resolveErrorMessage(
                        reasonCode = apiData.reasonCode,
                        reason = apiData.reason,
                        fallback = paymentFailedFallbackMessage()
                    )
                    CheckoutDetailsHandler.setErrorMessage(resolvedErrorMessage)
                    CheckoutDetailsHandler.setSessionFailed()
                    setIsBoxPayAnimationVisible(false)
                    if(notifyComponentsHandler != null) {
                        notifyComponentsHandler()
                    }
                }
                TransactionStatusEnum.EXPIRED ->{
                    CheckoutDetailsHandler.setSessionExpired()
                    setIsBoxPayAnimationVisible(false)
                    if(notifyComponentsHandler != null) {
                        notifyComponentsHandler()
                    }
                }
                else -> {
                    CheckoutDetailsHandler.setSessionFailed()
                    setIsBoxPayAnimationVisible(false)
                    if(notifyComponentsHandler != null) {
                        notifyComponentsHandler()
                    }
                }
            }
        }
        else -> {
            CheckoutDetailsHandler.setSessionFailed()
            setIsBoxPayAnimationVisible(false)
            if(notifyComponentsHandler != null) {
                notifyComponentsHandler()
            }
        }
    }
}

internal suspend fun handleUpiCollectFetchStatus(
    response : ApiResponse<FetchStatusResponse>,
    setIsBoxPayAnimationVisible : (Boolean) -> Unit,
    notifyComponentsHandler : (() -> Unit)? = null
) {
    when(response) {
        is ApiResponse.Error -> {
            CheckoutDetailsHandler.setErrorMessage()
            CheckoutDetailsHandler.setSessionFailed()
            setIsBoxPayAnimationVisible(false)
            if(notifyComponentsHandler != null) {
                notifyComponentsHandler()
            }
        }
        is ApiResponse.Success -> {
            val apiData = response.data
            val status = getFetchStatus(apiData.status)
            val transactionId = apiData.transactionId

            CheckoutDetailsHandler.setStatusAndTransID(
                status =  status.name,
                transactionId = transactionId
            )

            when (status){
                TransactionStatusEnum.SUCCESS -> {
                    CheckoutDetailsHandler.setTimeAndPaymentMethod(
                        timeStamp = response.data.transactionTimestampLocale,
                        paymentMethod = response.data.paymentMethod.brand ?: ""
                    )
                    CheckoutDetailsHandler.setSessionSuccess()
                    setIsBoxPayAnimationVisible(false)
                    if(notifyComponentsHandler != null) {
                        notifyComponentsHandler()
                    }
                }
                TransactionStatusEnum.FAILED -> {
                    val resolvedErrorMessage = resolveErrorMessage(
                        reasonCode = apiData.reasonCode,
                        reason = apiData.reason,
                        fallback = paymentFailedFallbackMessage()
                    )
                    CheckoutDetailsHandler.setErrorMessage(resolvedErrorMessage)
                    CheckoutDetailsHandler.setSessionFailed()
                    setIsBoxPayAnimationVisible(false)
                    if(notifyComponentsHandler != null) {
                        notifyComponentsHandler()
                    }
                }
                TransactionStatusEnum.EXPIRED ->{
                    CheckoutDetailsHandler.setSessionExpired()
                    setIsBoxPayAnimationVisible(false)
                    if(notifyComponentsHandler != null) {
                        notifyComponentsHandler()
                    }
                }
                else -> {
                    // no operation
                }
            }
        }
        else -> {
            CheckoutDetailsHandler.setSessionFailed()
            setIsBoxPayAnimationVisible(false)
            if(notifyComponentsHandler != null) {
                notifyComponentsHandler()
            }
        }
    }
}
