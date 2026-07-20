package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.implementation.ApiServiceImpl
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.service.ApiService
import com.crossplatform.sdk.domain.repo.FetchStatusRepo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class FetchStatusRepoImpl (
    private val apiService: ApiService = ApiServiceImpl(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FetchStatusRepo {
    override suspend fun fetchStatus(): ApiResponse<FetchStatusResponse> = withContext(ioDispatcher) {
        apiService.fetchStatus()
    }

    override suspend fun autoRetryInitiatePayment(
        transactionId : String
    ) : ApiResponse<PaymentMethodPostResponse> = withContext(ioDispatcher) {
        apiService.autoRetryInitiatePayment(transactionId)
    }
}