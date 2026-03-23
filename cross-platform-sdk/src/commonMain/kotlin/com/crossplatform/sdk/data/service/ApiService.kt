package com.crossplatform.sdk.data.service

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.AnalyticsResponse
import com.crossplatform.sdk.data.model.CheckoutDetails
import com.crossplatform.sdk.data.model.FetchCardDetails
import com.crossplatform.sdk.data.model.FetchSavedAddress
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse

interface ApiService {
    suspend fun getSessionDetails() : ApiResponse<CheckoutDetails>

    suspend fun callUiAnalytics(
        uiEvent: String,
        screenName : String,
        message : String
    ) : ApiResponse<AnalyticsResponse>

    suspend fun cardPostRequest(
        cardNumber : String,
        cvv : String,
        cardName : String,
        expiry : String,
        nickName : String?,
        isSaveInstrumentCheckboxClicked : Boolean,
        isSICheckboxClicked : Boolean
    ) : ApiResponse<PaymentMethodPostResponse>

    suspend fun fetchCardDetails(
        cardNumber : String
    ) : ApiResponse<FetchCardDetails>

    suspend fun deleteSavedAddress(
        addressRef : String
    ) : ApiResponse<FetchSavedAddress>

    suspend fun methodsPostRequest(
        instrumentDetails : String
    ) : ApiResponse<PaymentMethodPostResponse>

    suspend fun fetchPaymentMethods() : ApiResponse<PaymentMethodPostResponse>

    suspend fun fetchStatus() : ApiResponse<FetchStatusResponse>

}