package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.implementation.ApiServiceImpl
import com.crossplatform.sdk.data.model.CheckoutDetails
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
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
        shopperVpa: String?
    ): ApiResponse<PaymentMethodPostResponse> =withContext(ioDispatcher) {
        apiService.upiCollectPostRequest(
            type = type,
            instrumentRef = instrumentRef,
            shopperVpa = shopperVpa
        )
    }

    override suspend fun fetchStatus(): ApiResponse<FetchStatusResponse> = withContext(ioDispatcher) {
        apiService.fetchStatus()
    }

}