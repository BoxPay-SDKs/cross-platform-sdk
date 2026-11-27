package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AutoRefundTimerResponse
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.NativeOtpResponse
import com.crossplatform.sdk.fakes.FakeCallUIAnalyticsRepo
import com.crossplatform.sdk.fakes.FakeCardScreenRepo
import com.crossplatform.sdk.fakes.FakeFetchStatusRepo
import com.crossplatform.sdk.fakes.FakeOtherPaymentMethodRepo
import com.crossplatform.sdk.domain.model.PayNowUiState
import com.crossplatform.sdk.presentation.sharedContext.AutoRefundHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Call-site coverage for auto refund in every payment-method view model other than
 * Main / Elements (see [AutoRefundCallSitesTest]): Card (incl. native OTP), Wallet, EMI,
 * BNPL, NetBanking and PayNow.
 *
 * Each one must route a PENDING status through `fetchStatusWithAutoRefund`, which calls the
 * auto-refund timer API when `configs.autoRefundEnabled` is on and leaves behaviour untouched
 * when it is off.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AutoRefundPaymentMethodCallSitesTest {

    private lateinit var fetchStatusRepo: FakeFetchStatusRepo
    private lateinit var otherRepo: FakeOtherPaymentMethodRepo
    private lateinit var cardRepo: FakeCardScreenRepo
    private lateinit var analyticsRepo: FakeCallUIAnalyticsRepo

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        CheckoutDetailsHandler.resetToDefault()
        AutoRefundHandler.resetForTest()
        otherRepo = FakeOtherPaymentMethodRepo()
        cardRepo = FakeCardScreenRepo()
        analyticsRepo = FakeCallUIAnalyticsRepo()
        fetchStatusRepo = FakeFetchStatusRepo().apply {
            startAutoRefundTimerResult = ApiResponse.Success(
                AutoRefundTimerResponse(
                    token = "token",
                    transactionId = "txn_1",
                    timerStarted = true,
                    autoRefundAt = "2026-11-23T10:46:12.835551837Z",
                    autoRefundTimeoutSeconds = 30,
                ),
                responseCode = 200,
            )
        }
    }

    @AfterTest
    fun tearDown() {
        AutoRefundHandler.resetForTest()
        CheckoutDetailsHandler.resetToDefault()
        Dispatchers.resetMain()
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun status(value: String) = ApiResponse.Success(
        FetchStatusResponse(
            status = value,
            transactionId = "txn_1",
            reasonCode = "",
            reason = "",
            transactionTimestampLocale = "01/06/2025 10:00:00",
            retryable = false,
            paymentMethod = FetchStatusResponse.PaymentMethod(id = "pm_1", brand = "VISA"),
        ),
        responseCode = 200,
    )

    private fun TestScope.useVirtualClock() {
        AutoRefundHandler.clock = { testScheduler.currentTime }
    }

    /** PENDING, then SUCCESS: with the flag on the timer API must be called with the transaction id. */
    private suspend fun TestScope.assertPendingStartsTimer(callFetchStatus: (String) -> Unit) {
        useVirtualClock()
        AutoRefundHandler.isEnabled = true
        fetchStatusRepo.fetchStatusQueue.addAll(listOf(status("PENDING"), status("SUCCESS")))

        callFetchStatus("inquiry_token")
        advanceUntilIdle()

        assertEquals("txn_1", fetchStatusRepo.lastAutoRefundTransactionId)
        assertTrue(fetchStatusRepo.fetchStatusCallCount >= 2)
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertNull(AutoRefundHandler.secondsLeft.value)
        assertFalse(AutoRefundHandler.isRunning)
    }

    /** PENDING with the flag off: the timer API must never be called. */
    private suspend fun TestScope.assertPendingWithFlagOffSkipsTimer(callFetchStatus: (String) -> Unit) {
        useVirtualClock()
        AutoRefundHandler.isEnabled = false
        fetchStatusRepo.fetchStatusResult = status("PENDING")

        callFetchStatus("inquiry_token")
        runCurrent()

        assertNull(fetchStatusRepo.lastAutoRefundTransactionId)
        assertNull(AutoRefundHandler.secondsLeft.value)
    }

    // ── CardScreenViewModel ──────────────────────────────────────────────────

    private fun cardViewModel() = CardScreenViewModel(cardRepo, fetchStatusRepo, analyticsRepo)

    @Test
    fun `card - pending status starts the auto refund timer`() = runTest {
        val viewModel = cardViewModel()
        assertPendingStartsTimer { viewModel.callFetchStatus(it) }
    }

    @Test
    fun `card - pending status with the flag off skips the auto refund timer`() = runTest {
        val viewModel = cardViewModel()
        assertPendingWithFlagOffSkipsTimer { viewModel.callFetchStatus(it) }
    }

    @Test
    fun `card - native otp submit polls the status and starts the auto refund timer on pending`() = runTest {
        useVirtualClock()
        AutoRefundHandler.isEnabled = true
        cardRepo.submitOtpResult = ApiResponse.Success(NativeOtpResponse(success = true), responseCode = 200)
        fetchStatusRepo.fetchStatusQueue.addAll(listOf(status("PENDING"), status("SUCCESS")))
        val viewModel = cardViewModel()

        viewModel.submitOtp(otp = "123456", isTestEnv = true)
        advanceUntilIdle()

        assertEquals("txn_1", fetchStatusRepo.lastAutoRefundTransactionId)
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        viewModel.stopFetchStatusPolling()
    }

    @Test
    fun `card - native otp submit with the flag off never calls the auto refund api`() = runTest {
        useVirtualClock()
        AutoRefundHandler.isEnabled = false
        cardRepo.submitOtpResult = ApiResponse.Success(NativeOtpResponse(success = true), responseCode = 200)
        fetchStatusRepo.fetchStatusResult = status("PENDING")
        val viewModel = cardViewModel()

        viewModel.submitOtp(otp = "123456", isTestEnv = true)
        runCurrent()
        viewModel.stopFetchStatusPolling() // the poll loop would otherwise keep going on virtual time

        assertNull(fetchStatusRepo.lastAutoRefundTransactionId)
    }

    @Test
    fun `card - a plain upi collect status check does not start the auto refund timer`() = runTest {
        useVirtualClock()
        AutoRefundHandler.isEnabled = true
        fetchStatusRepo.fetchStatusResult = status("PENDING")
        val viewModel = cardViewModel()

        viewModel.callUpiCollectFetchStatue("inquiry_token") // autoRefundOnPending defaults to false
        runCurrent()

        assertNull(fetchStatusRepo.lastAutoRefundTransactionId)
    }

    // ── WalletViewModel ──────────────────────────────────────────────────────

    @Test
    fun `wallet - pending status starts the auto refund timer`() = runTest {
        val viewModel = WalletViewModel(otherRepo, analyticsRepo, fetchStatusRepo)
        assertPendingStartsTimer { viewModel.callFetchStatus(it) }
    }

    @Test
    fun `wallet - pending status with the flag off skips the auto refund timer`() = runTest {
        val viewModel = WalletViewModel(otherRepo, analyticsRepo, fetchStatusRepo)
        assertPendingWithFlagOffSkipsTimer { viewModel.callFetchStatus(it) }
    }

    // ── EMIScreenViewModel ───────────────────────────────────────────────────

    private fun emiViewModel() = EMIScreenViewModel(otherRepo, cardRepo, analyticsRepo, fetchStatusRepo)

    @Test
    fun `emi - pending status starts the auto refund timer`() = runTest {
        val viewModel = emiViewModel()
        assertPendingStartsTimer { viewModel.callFetchStatus(it) }
    }

    @Test
    fun `emi - pending status with the flag off skips the auto refund timer`() = runTest {
        val viewModel = emiViewModel()
        assertPendingWithFlagOffSkipsTimer { viewModel.callFetchStatus(it) }
    }

    // ── BNPLViewModel ────────────────────────────────────────────────────────

    @Test
    fun `bnpl - pending status starts the auto refund timer`() = runTest {
        val viewModel = BNPLViewModel(otherRepo, fetchStatusRepo, analyticsRepo)
        assertPendingStartsTimer { viewModel.callFetchStatus(it) }
    }

    @Test
    fun `bnpl - pending status with the flag off skips the auto refund timer`() = runTest {
        val viewModel = BNPLViewModel(otherRepo, fetchStatusRepo, analyticsRepo)
        assertPendingWithFlagOffSkipsTimer { viewModel.callFetchStatus(it) }
    }

    // ── NetBankingViewModel ──────────────────────────────────────────────────

    @Test
    fun `net banking - pending status starts the auto refund timer`() = runTest {
        val viewModel = NetBankingViewModel(otherRepo, analyticsRepo, fetchStatusRepo)
        assertPendingStartsTimer { viewModel.callFetchStatus(it) }
    }

    @Test
    fun `net banking - pending status with the flag off skips the auto refund timer`() = runTest {
        val viewModel = NetBankingViewModel(otherRepo, analyticsRepo, fetchStatusRepo)
        assertPendingWithFlagOffSkipsTimer { viewModel.callFetchStatus(it) }
    }

    // ── PayNowScreenViewModel (QR window end) ────────────────────────────────

    private fun payNowViewModel() = PayNowScreenViewModel(otherRepo, analyticsRepo, fetchStatusRepo)

    @Test
    fun `paynow - qr expiry with a pending status starts the auto refund timer`() = runTest {
        useVirtualClock()
        AutoRefundHandler.isEnabled = true
        fetchStatusRepo.fetchStatusQueue.addAll(listOf(status("PENDING"), status("SUCCESS")))
        val viewModel = payNowViewModel()

        viewModel.markExpired()
        advanceUntilIdle()

        assertEquals(PayNowUiState.Expired, viewModel.qrState.value)
        assertEquals("txn_1", fetchStatusRepo.lastAutoRefundTransactionId)
    }

    @Test
    fun `paynow - qr expiry with the flag off only marks the qr expired`() = runTest {
        useVirtualClock()
        AutoRefundHandler.isEnabled = false
        fetchStatusRepo.fetchStatusResult = status("PENDING")
        val viewModel = payNowViewModel()

        viewModel.markExpired()
        advanceUntilIdle()

        assertEquals(PayNowUiState.Expired, viewModel.qrState.value)
        assertEquals(0, fetchStatusRepo.fetchStatusCallCount)
        assertNull(fetchStatusRepo.lastAutoRefundTransactionId)
    }
}
