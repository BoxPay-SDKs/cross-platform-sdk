package com.crossplatform.sdk.presentation.sharedContext

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import com.crossplatform.sdk.presentation.getStatus
import com.crossplatform.sdk.presentation.resolveErrorMessage

fun handleFetchStatus(
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
            val status = getStatus(apiData.status)
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
                        fallback = "You may have cancelled the payment or there was a delay in response. Please retry."
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
                    if(response.data.retryable) {
                        onAutoRetry()
                        return
                    }
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

fun handleUpiCollectFetchStatus(
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
            val status = getStatus(apiData.status)
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
                        fallback = "You may have cancelled the payment or there was a delay in response. Please retry."
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
