package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.AnalyticsResponse
import com.crossplatform.sdk.fakes.FakeApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CallUIAnalyticsRepoImplTest {

    @Test
    fun `forwards event name, screen name and message to the api service unchanged`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextAnalyticsResponse = ApiResponse.Success(AnalyticsResponse(id = "evt_1"), responseCode = 200)
        }
        val repo = CallUIAnalyticsRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

        val result = repo.callUiAnalytics(uiEvent = "CLICK", screenName = "CardScreen", message = "Pay button tapped")

        assertIs<ApiResponse.Success<AnalyticsResponse>>(result)
        assertEquals("evt_1", result.data.id)
        assertEquals("callUiAnalytics(CLICK, CardScreen, Pay button tapped)", fakeApi.callLog.single())
    }

    @Test
    fun `propagates an error response from the api service`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextAnalyticsResponse = ApiResponse.Error(message = "network down")
        }
        val repo = CallUIAnalyticsRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

        val result = repo.callUiAnalytics(uiEvent = "VIEW", screenName = "MainScreen", message = "")

        assertIs<ApiResponse.Error>(result)
        assertEquals("network down", result.message)
    }
}
