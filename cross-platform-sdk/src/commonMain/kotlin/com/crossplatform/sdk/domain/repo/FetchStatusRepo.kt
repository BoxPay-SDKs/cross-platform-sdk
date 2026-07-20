package com.crossplatform.sdk.domain.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse

interface FetchStatusRepo {
    suspend fun fetchStatus() : ApiResponse<FetchStatusResponse>
    suspend fun autoRetryInitiatePayment(
        transactionId : String
    ) : ApiResponse<PaymentMethodPostResponse>
}