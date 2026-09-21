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

internal suspend fun handleFetchStatus(
    response : ApiResponse<FetchStatusResponse>,
    setIsBoxPayAnimationVisible : (Boolean) -> Unit,
    onAutoRetry : () -> Unit
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
                        fallback = getString(Res.string.payment_failed_error_messages)
                    )
                    CheckoutDetailsHandler.setErrorMessage(resolvedErrorMessage)
                    CheckoutDetailsHandler.setSessionFailed()
                    setIsBoxPayAnimationVisible(false)
                }
                TransactionStatusEnum.EXPIRED ->{
                    CheckoutDetailsHandler.setSessionExpired()
                    setIsBoxPayAnimationVisible(false)
                }
                else -> {
                    CheckoutDetailsHandler.setSessionFailed()
                    setIsBoxPayAnimationVisible(false)
                }
            }
        }
        else -> {
            CheckoutDetailsHandler.setSessionFailed()
            setIsBoxPayAnimationVisible(false)
        }
    }
}

internal suspend fun handleUpiCollectFetchStatus(
    response : ApiResponse<FetchStatusResponse>,
    setIsBoxPayAnimationVisible : (Boolean) -> Unit
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
                    val resolvedErrorMessage = resolveErrorMessage(
                        reasonCode = apiData.reasonCode,
                        reason = apiData.reason,
                        fallback = getString(Res.string.payment_failed_error_messages)
                    )
                    CheckoutDetailsHandler.setErrorMessage(resolvedErrorMessage)
                    CheckoutDetailsHandler.setSessionFailed()
                    setIsBoxPayAnimationVisible(false)
                }
                TransactionStatusEnum.EXPIRED ->{
                    CheckoutDetailsHandler.setSessionExpired()
                    setIsBoxPayAnimationVisible(false)
                }
                else -> {
                    // no operation
                }
            }
        }
        else -> {
            CheckoutDetailsHandler.setSessionFailed()
            setIsBoxPayAnimationVisible(false)
        }
    }
}
