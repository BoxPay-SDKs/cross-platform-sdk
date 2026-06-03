package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.implementation.ApiServiceImpl
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.FetchSurchargeResponse
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.RecommendedInstrumentsResponse
import com.crossplatform.sdk.data.model.SessionDetails
import com.crossplatform.sdk.data.service.ApiService
import com.crossplatform.sdk.domain.repo.MainScreenRepo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class MainScreenRepoImpl(
    private val apiService: ApiService = ApiServiceImpl(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO // Injectable dispatcher
) : MainScreenRepo {
    override suspend fun getSessionDetails(): ApiResponse<SessionDetails> = withContext(ioDispatcher) {
        apiService.getSessionDetails()
    }

    override suspend fun postUpiIntentRequest(
        type: String,
        upiApp: String
    ): ApiResponse<PaymentMethodPostResponse> =withContext(ioDispatcher) {
        apiService.upiIntentPostRequest(
            type = type,
            upiApp = upiApp
        )
    }

    override suspend fun postUpiCollectRequest(
        type: String,
        instrumentRef: String?,
        shopperVpa: String?,
        saveInstrument: Boolean?,
    ): ApiResponse<PaymentMethodPostResponse> =withContext(ioDispatcher) {
        apiService.upiCollectPostRequest(
            type = type,
            instrumentRef = instrumentRef,
            shopperVpa = shopperVpa,
            saveInstrument = saveInstrument
        )
    }

    override suspend fun fetchRecommendedInstruments(): ApiResponse<List<RecommendedInstrumentsResponse>> = withContext(ioDispatcher) {
        apiService.getRecommendedInstruments()
    }

    override suspend fun postSavedCardRequest(
        instrumentRef: String,
        isSICheckboxChecked: Boolean
    ): ApiResponse<PaymentMethodPostResponse> = withContext(ioDispatcher) {
        apiService.savedCardPostRequest(
            instrumentRef = instrumentRef,
            isSICheckboxClicked = isSICheckboxChecked
        )
    }

    override suspend fun getSurcharge(
        amount: Double,
        currencyCode: String
    ): ApiResponse<FetchSurchargeResponse> = withContext(ioDispatcher) {
        apiService.getSurcharge(
            amount = amount,
            currencyCode = currencyCode
        )
    }

    override suspend fun deleteSavedCard(id: String): ApiResponse<RecommendedInstrumentsResponse> = withContext(ioDispatcher) {
        apiService.deleteSavedCard(id)
    }

}