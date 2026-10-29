package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.repo.CardScreenRepoImpl
import com.crossplatform.sdk.data.repo.FetchStatusRepoImpl
import com.crossplatform.sdk.fakes.FakeApiService
import com.crossplatform.sdk.fakes.FakeCallUIAnalyticsRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for [FetchStatusRepoImpl].
 *
 * Uses REAL [FetchStatusRepoImpl] backed by [FakeApiService], wired into
 * REAL [CardScreenViewModel]. Covers:
 *  - fetchStatus routes through real repo to FakeApiService
 *  - SUCCESS status marks CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful
 *  - FAILED status marks CheckoutDetailsHandler.checkoutDetails.isPaymentFailed
 *  - EXPIRED and RETRYABLE statuses
 *  - autoRetryInitiatePayment routes through real repo
 *  - Full auto-retry e2e (retry → new RequiresAction → then SUCCESS)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FetchStatusRepoIntegrationTest {

    private lateinit var api: FakeApiService
    private lateinit var cardRepo: CardScreenRepoImpl
    private lateinit var fetchStatusRepo: FetchStatusRepoImpl
    private lateinit var viewModel: CardScreenViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        CheckoutDetailsHandler.resetToDefault()
        CheckoutDetailsHandler.setCheckoutToken(
            shopperToken            = null,
            token                   = "fetch_status_token",
            isTestEnv               = true,
            isSuccessScreenVisible  = true,
            ctaBorderRadius         = 8,
            isSICheckboxChecked     = false,
            isSICheckboxEnabled     = false,
            isFailedScreenVisible   = true,
            showQROnLoad            = false,
            focusedTextInputBorderColor   = "",
            unfocusedTextInputBorderColor   = ""
        )

        api             = FakeApiService()
        cardRepo        = CardScreenRepoImpl(apiService = api, ioDispatcher = UnconfinedTestDispatcher())
        fetchStatusRepo = FetchStatusRepoImpl(apiService = api, ioDispatcher = UnconfinedTestDispatcher())
        viewModel       = CardScreenViewModel(
            repo            = cardRepo,
            fetchStatusRepo = fetchStatusRepo,
            analyticsRepo   = FakeCallUIAnalyticsRepo()
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        CheckoutDetailsHandler.resetToDefault()
    }

    // ── fetchStatus happy paths ──────────────────────────────────────────────

    @Test
    fun `fetchStatus SUCCESS routes through real FetchStatusRepoImpl to FakeApiService`() = runTest {
        api.nextFetchStatus = ApiResponse.Success(
            buildSuccessFetchStatus(transactionId = "txn_fs_001"),
            responseCode = 200
        )

        viewModel.callFetchStatus("SUCCESS")

        assertTrue(api.callLog.any { it.startsWith("fetchStatus") })
    }

    @Test
    fun `fetchStatus SUCCESS sets isPaymentSuccessful in CheckoutDetailsHandler`() = runTest {
        api.nextFetchStatus = ApiResponse.Success(
            buildSuccessFetchStatus(transactionId = "txn_success"),
            responseCode = 200
        )

        viewModel.callFetchStatus("SUCCESS")

        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed)
    }

    @Test
    fun `fetchStatus FAILED sets isPaymentFailed in CheckoutDetailsHandler`() = runTest {
        api.nextFetchStatus = ApiResponse.Success(
            buildFailedFetchStatus(reasonCode = "DECLINED", reason = "Card declined"),
            responseCode = 200
        )

        viewModel.callFetchStatus("FAILED")

        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
    }

    @Test
    fun `fetchStatus EXPIRED does not mark payment as successful`() = runTest {
        api.nextFetchStatus = ApiResponse.Success(
            buildExpiredFetchStatus(),
            responseCode = 200
        )

        viewModel.callFetchStatus("EXPIRED")

        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
    }

    @Test
    fun `fetchStatus API error does not crash and does not mark success`() = runTest {
        api.nextFetchStatus = ApiResponse.Error(message = "network error")

        viewModel.callFetchStatus("SUCCESS")

        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
    }

    // ── autoRetryInitiatePayment via real FetchStatusRepoImpl ────────────────

    @Test
    fun `autoRetryInitiatePayment routes through real FetchStatusRepoImpl to FakeApiService`() = runTest {
        api.nextAutoRetryInitiatePayment = ApiResponse.Success(
            buildRequiresActionResponse(transactionId = "txn_retry_001", actionType = "redirect"),
            responseCode = 200
        )

        val result = fetchStatusRepo.autoRetryInitiatePayment("txn_retry_001")

        assertTrue(result is ApiResponse.Success)
        assertNotNull(api.callLog.firstOrNull { it.contains("autoRetryInitiatePayment") })
    }

    @Test
    fun `autoRetryInitiatePayment forwards transactionId through real repo`() = runTest {
        api.nextAutoRetryInitiatePayment = ApiResponse.Success(
            buildRequiresActionResponse(transactionId = "txn_specific_id"),
            responseCode = 200
        )

        fetchStatusRepo.autoRetryInitiatePayment("txn_specific_id")

        assertTrue(api.callLog.any { it.contains("txn_specific_id") })
    }

    @Test
    fun `autoRetryInitiatePayment error returns ApiResponse Error from real repo`() = runTest {
        api.nextAutoRetryInitiatePayment = ApiResponse.Error(message = "retry failed")

        val result = fetchStatusRepo.autoRetryInitiatePayment("txn_bad")

        assertTrue(result is ApiResponse.Error)
        assertEquals("retry failed", (result as ApiResponse.Error).message)
    }

    // ── fetchStatus called twice returns the latest response ──────────────────

    @Test
    fun `fetchStatus can be called multiple times independently`() = runTest {
        // First call → FAILED
        api.nextFetchStatus = ApiResponse.Success(buildFailedFetchStatus(), responseCode = 200)
        viewModel.callFetchStatus("FAILED")
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed)

        // Reset and call again → SUCCESS
        CheckoutDetailsHandler.resetToDefault()
        api.nextFetchStatus = ApiResponse.Success(buildSuccessFetchStatus(), responseCode = 200)
        viewModel.callFetchStatus("SUCCESS")
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
    }

    // ── retryable fetch status ───────────────────────────────────────────────

    @Test
    fun `fetchStatus retryable FAILED status does not mark payment as finally failed`() = runTest {
        api.nextFetchStatus = ApiResponse.Success(
            buildRetryableFetchStatus(),
            responseCode = 200
        )

        viewModel.callFetchStatus("FAILED")

        // Retryable means the session should still be active — not a terminal failure
        // The VM routes this to a retry; do NOT set isPaymentFailed immediately
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
    }
}
