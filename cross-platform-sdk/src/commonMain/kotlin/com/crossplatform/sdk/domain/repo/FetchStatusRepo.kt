package com.crossplatform.sdk.domain.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.FetchStatusResponse

interface FetchStatusRepo {
    suspend fun fetchStatus() : ApiResponse<FetchStatusResponse>
}