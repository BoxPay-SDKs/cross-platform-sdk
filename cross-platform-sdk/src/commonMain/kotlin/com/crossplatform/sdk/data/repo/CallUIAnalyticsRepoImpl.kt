package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.implementation.ApiServiceImpl
import com.crossplatform.sdk.data.model.AnalyticsResponse
import com.crossplatform.sdk.data.service.ApiService
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class CallUIAnalyticsRepoImpl (
    private val apiService: ApiService = ApiServiceImpl(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO // Injectable dispatcher
) : CallUIAnalyticsRepo {
    override suspend fun callUiAnalytics(
        uiEvent: String,
        screenName: String,
        message: String
    ): ApiResponse<AnalyticsResponse> = withContext(ioDispatcher) {
        apiService.callUiAnalytics(
            uiEvent = uiEvent,
            screenName = screenName,
            message = message
        )
    }

}