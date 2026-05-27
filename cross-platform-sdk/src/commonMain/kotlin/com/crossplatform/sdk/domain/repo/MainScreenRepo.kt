package com.crossplatform.sdk.domain.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.FetchSurchargeResponse
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.RecommendedInstrumentsResponse
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
        shopperVpa : String?,
        saveInstrument : Boolean?
    ) : ApiResponse<PaymentMethodPostResponse>

    suspend fun fetchStatus() : ApiResponse<FetchStatusResponse>

    suspend fun fetchRecommendedInstruments() : ApiResponse<List<RecommendedInstrumentsResponse>>

    suspend fun postSavedCardRequest(instrumentRef : String, isSICheckboxChecked : Boolean) : ApiResponse<PaymentMethodPostResponse>

    suspend fun getSurcharge(amount : Double, currencyCode : String) : ApiResponse<FetchSurchargeResponse>

    suspend fun deleteSavedCard(id : String) : ApiResponse<RecommendedInstrumentsResponse>
}