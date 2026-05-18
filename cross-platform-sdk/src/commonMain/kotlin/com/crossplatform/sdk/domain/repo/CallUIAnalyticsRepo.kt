package com.crossplatform.sdk.domain.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.AnalyticsResponse

interface CallUIAnalyticsRepo {
    suspend fun callUiAnalytics(
        uiEvent: String,
        screenName: String,
        message: String
    ) : ApiResponse<AnalyticsResponse>
}