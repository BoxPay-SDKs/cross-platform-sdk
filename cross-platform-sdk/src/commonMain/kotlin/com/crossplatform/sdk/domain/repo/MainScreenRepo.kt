package com.crossplatform.sdk.domain.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.SessionDetails

interface MainScreenRepo {
    suspend fun getSessionDetails() : ApiResponse<SessionDetails>

    suspend fun postUpiIntentRequest(
        type : String,
        upiApp : String
    ) : ApiResponse<PaymentMethodPostResponse>

    suspend fun postUpiCollectRequest(
        type : String,
        instrumentRef : String?,
        shopperVpa : String?
    ) : ApiResponse<PaymentMethodPostResponse>

    suspend fun fetchStatus() : ApiResponse<FetchStatusResponse>
}