package com.crossplatform.sdk.presentation.sharedContext

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import com.crossplatform.sdk.presentation.getFetchStatus
import com.crossplatform.sdk.presentation.resolveErrorMessage

internal fun handleFetchStatus(
    response : ApiResponse<FetchStatusResponse>,
    setIsBoxPayAnimationVisible : (Boolean) -> Unit,
    onAutoRetry : () -> Unit
) {
    when(response) {
        is ApiResponse.Error -> {
            println("======response  in fetch status error === $response")
            CheckoutDetailsHandler.setErrorMessage()
            CheckoutDetailsHandler.setSessionFailed()
            setIsBoxPayAnimationVisible(false)
        }
        is ApiResponse.Success -> {
            val apiData = response.data
            println("======response  in fetch status success === ${response.data}")
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
                    println("======response  in fetch status success in else === ${response.data}")
                    CheckoutDetailsHandler.setSessionFailed()
                    setIsBoxPayAnimationVisible(false)
                }
            }
        }
        else -> {
            println("======response  in fetch statussss === ${response}")
            CheckoutDetailsHandler.setSessionFailed()
            setIsBoxPayAnimationVisible(false)
        }
    }
}

internal fun handleUpiCollectFetchStatus(
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
