package com.crossplatform.sdk.data.service

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.AnalyticsResponse
import com.crossplatform.sdk.data.model.FetchCardDetails
import com.crossplatform.sdk.data.model.FetchSavedAddress
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.SessionDetails

interface ApiService {
    suspend fun getSessionDetails() : ApiResponse<SessionDetails>

    suspend fun callUiAnalytics(
        uiEvent: String,
        screenName : String,
        message : String
    ) : ApiResponse<AnalyticsResponse>

    suspend fun cardPostRequest(
        type : String,
        cardNumber : String,
        cvv : String,
        cardName : String,
        expiry : String,
        nickName : String?,
        isSaveInstrumentCheckboxClicked : Boolean,
        isSICheckboxClicked : Boolean?
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

    suspend fun fetchPaymentMethods() : ApiResponse<List<PaymentMethod>>

    suspend fun fetchStatus() : ApiResponse<FetchStatusResponse>

    suspend fun upiIntentPostRequest(
        type: String,
        upiApp : String
    ) : ApiResponse<PaymentMethodPostResponse>

    suspend fun upiCollectPostRequest(
        type : String,
        instrumentRef : String?,
        shopperVpa : String?
    ) : ApiResponse<PaymentMethodPostResponse>

    suspend fun upiQrPostRequest(
        type : String,
    ) : ApiResponse<PaymentMethodPostResponse>

    suspend fun savedCardPostRequest(
        instrumentRef: String,
        isSICheckboxClicked: Boolean?
    ) : ApiResponse<PaymentMethodPostResponse>

}