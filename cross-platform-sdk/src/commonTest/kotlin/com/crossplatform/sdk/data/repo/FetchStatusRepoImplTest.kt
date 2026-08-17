package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.PaymentActions
import com.crossplatform.sdk.data.model.TransactionStatus
import com.crossplatform.sdk.data.model.Method
import com.crossplatform.sdk.fakes.FakeApiService
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * [FetchStatusRepoImplTest] verifies the repo is a thin, faithful pass-through
 * to [com.crossplatform.sdk.data.service.ApiService] — it forwards arguments
 * unchanged and returns whatever the service returns, for both the success
 * and error paths.
 */
class FetchStatusRepoImplTest {

    private fun statusResponse(status: String = "APPROVED") = FetchStatusResponse(
        status = status,
        transactionId = "txn_123",
        reasonCode = "",
        reason = "",
        transactionTimestampLocale = "12 Aug 2026",
        retryable = false,
        paymentMethod = FetchStatusResponse.PaymentMethod(id = "pm_1", type = "Card", brand = "VISA"),
    )

    private fun postResponse() = PaymentMethodPostResponse(
        transactionId = "txn_999",
        transactionTimestampLocale = "12 Aug 2026",
        status = TransactionStatus(status = "REQUIRESACTION", reason = "", reasonCode = ""),
        actions = listOf(PaymentActions(method = "GET", url = "https://pay.example.com")),
        paymentMethod = Method(type = "Card", brand = "VISA"),
    )

    @Test
    fun `fetchStatus returns the success response from the api service unchanged`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextFetchStatus = ApiResponse.Success(statusResponse(status = "APPROVED"), responseCode = 200)
        }
        val repo = FetchStatusRepoImpl(apiService = fakeApi, ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined)

        val result = repo.fetchStatus()

        assertIs<ApiResponse.Success<FetchStatusResponse>>(result)
        assertEquals("APPROVED", result.data.status)
        assertEquals("txn_123", result.data.transactionId)
        assertTrue(fakeApi.callLog.contains("fetchStatus()"))
    }

    @Test
    fun `fetchStatus propagates an error response without swallowing it`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextFetchStatus = ApiResponse.Error(message = "Network error: Please check your internet connection.")
        }
        val repo = FetchStatusRepoImpl(apiService = fakeApi, ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined)

        val result = repo.fetchStatus()

        assertIs<ApiResponse.Error>(result)
        assertEquals("Network error: Please check your internet connection.", result.message)
    }

    @Test
    fun `autoRetryInitiatePayment forwards the transaction id to the api service`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextAutoRetryInitiatePayment = ApiResponse.Success(postResponse(), responseCode = 200)
        }
        val repo = FetchStatusRepoImpl(apiService = fakeApi, ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined)

        val result = repo.autoRetryInitiatePayment(transactionId = "txn_abc")

        assertIs<ApiResponse.Success<PaymentMethodPostResponse>>(result)
        assertTrue(fakeApi.callLog.any { it.contains("autoRetryInitiatePayment(txn_abc)") })
    }
}
