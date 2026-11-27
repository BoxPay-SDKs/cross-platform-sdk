package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AutoRefundTimerResponse
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.fakes.FakeCallUIAnalyticsRepo
import com.crossplatform.sdk.fakes.FakeCardScreenRepo
import com.crossplatform.sdk.fakes.FakeFetchStatusRepo
import com.crossplatform.sdk.fakes.FakeInstantOfferRepo
import com.crossplatform.sdk.fakes.FakeMainScreenRepo
import com.crossplatform.sdk.fakes.FakeOtherPaymentMethodRepo
import com.crossplatform.sdk.presentation.sharedContext.AutoRefundHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
 * Call-site coverage for auto refund. [HandleAutoRefundTest] covers the shared flow itself;
 * these tests prove the view models actually route through it:
 *  - a PENDING status in the main checkout and in the Elements component starts the
 *    auto-refund timer (and only when the session flag is on),
 *  - [AutoRefundHandler.isEnabled] follows `configs.autoRefundEnabled` and is reset between sessions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AutoRefundCallSitesTest {

    private lateinit var mainScreenRepo: FakeMainScreenRepo
    private lateinit var fetchStatusRepo: FakeFetchStatusRepo

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        CheckoutDetailsHandler.resetToDefault()
        AutoRefundHandler.resetForTest()
        mainScreenRepo = FakeMainScreenRepo().apply {
            surchargeResult = ApiResponse.Error(message = "no surcharge")
        }
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

    private fun setToken() {
        CheckoutDetailsHandler.setCheckoutToken(
            shopperToken = null,
            token = "session_token",
            isTestEnv = true,
            isSuccessScreenVisible = true,
            ctaBorderRadius = 8,
            isSICheckboxChecked = false,
            isSICheckboxEnabled = false,
            isFailedScreenVisible = true,
            showQROnLoad = false,
            focusedTextInputBorderColor = "",
            unfocusedTextInputBorderColor = "",
        )
    }

    private fun mainViewModel() = MainScreenViewModel(
        mainScreenRepo,
        FakeCallUIAnalyticsRepo(),
        FakeOtherPaymentMethodRepo(),
        FakeInstantOfferRepo(),
        fetchStatusRepo,
    )

    private fun elementsViewModel() = BoxPayElementsViewModel(
        mainScreenRepo,
        FakeCardScreenRepo(),
        FakeCallUIAnalyticsRepo(),
        fetchStatusRepo,
        FakeOtherPaymentMethodRepo(),
    )

    // ── main checkout: PENDING goes through fetchStatusWithAutoRefund ────────

    @Test
    fun `main checkout - pending status starts the auto refund timer and success is then handled`() = runTest {
        useVirtualClock()
        fetchStatusRepo.fetchStatusQueue.addAll(listOf(status("PENDING"), status("SUCCESS")))
        val viewModel = mainViewModel()
        // init{} -> loadSession() resets the flag, so enable it after construction
        AutoRefundHandler.isEnabled = true

        viewModel.callFetchStatus("inquiry_token")
        advanceUntilIdle()

        assertEquals("txn_1", fetchStatusRepo.lastAutoRefundTransactionId)
        assertTrue(fetchStatusRepo.fetchStatusCallCount >= 2)
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertNull(AutoRefundHandler.secondsLeft.value)
        assertFalse(AutoRefundHandler.isRunning)
    }

    @Test
    fun `main checkout - pending status with the flag off never calls the auto refund api`() = runTest {
        useVirtualClock()
        fetchStatusRepo.fetchStatusResult = status("PENDING")
        val viewModel = mainViewModel()
        // init{} -> loadSession() resets the flag, so enable it after construction
        AutoRefundHandler.isEnabled = false

        viewModel.callFetchStatus("inquiry_token")
        advanceUntilIdle()

        assertNull(fetchStatusRepo.lastAutoRefundTransactionId)
    }

    @Test
    fun `main checkout - upi window end with a pending status starts the auto refund timer`() = runTest {
        useVirtualClock()
        fetchStatusRepo.fetchStatusQueue.addAll(listOf(status("PENDING"), status("SUCCESS")))
        val viewModel = mainViewModel()
        // init{} -> loadSession() resets the flag, so enable it after construction
        AutoRefundHandler.isEnabled = true

        viewModel.onUpiWindowEnded()
        advanceUntilIdle()

        assertEquals("txn_1", fetchStatusRepo.lastAutoRefundTransactionId)
    }

    @Test
    fun `main checkout - upi window end with the flag off does nothing`() = runTest {
        useVirtualClock()
        fetchStatusRepo.fetchStatusResult = status("PENDING")
        val viewModel = mainViewModel()
        // init{} -> loadSession() resets the flag, so enable it after construction
        AutoRefundHandler.isEnabled = false

        viewModel.onUpiWindowEnded()
        advanceUntilIdle()

        assertEquals(0, fetchStatusRepo.fetchStatusCallCount)
        assertNull(fetchStatusRepo.lastAutoRefundTransactionId)
    }

    // ── Elements component: PENDING goes through fetchStatusWithAutoRefund ───

    @Test
    fun `elements - pending status starts the auto refund timer and success is then handled`() = runTest {
        useVirtualClock()
        fetchStatusRepo.fetchStatusQueue.addAll(listOf(status("PENDING"), status("SUCCESS")))
        val viewModel = elementsViewModel()
        // init{} -> loadSession() resets the flag, so enable it after construction
        AutoRefundHandler.isEnabled = true

        viewModel.callFetchStatus("inquiry_token")
        advanceUntilIdle()

        assertEquals("txn_1", fetchStatusRepo.lastAutoRefundTransactionId)
        assertTrue(fetchStatusRepo.fetchStatusCallCount >= 2)
        assertNull(AutoRefundHandler.secondsLeft.value)
        assertFalse(AutoRefundHandler.isRunning)
    }

    @Test
    fun `elements - pending status with the flag off never calls the auto refund api`() = runTest {
        useVirtualClock()
        fetchStatusRepo.fetchStatusResult = status("PENDING")
        val viewModel = elementsViewModel()
        // init{} -> loadSession() resets the flag, so enable it after construction
        AutoRefundHandler.isEnabled = false

        viewModel.callFetchStatus("inquiry_token")
        advanceUntilIdle()

        assertNull(fetchStatusRepo.lastAutoRefundTransactionId)
    }

    @Test
    fun `elements - upi window end with a pending status starts the auto refund timer`() = runTest {
        useVirtualClock()
        fetchStatusRepo.fetchStatusQueue.addAll(listOf(status("PENDING"), status("SUCCESS")))
        val viewModel = elementsViewModel()
        // init{} -> loadSession() resets the flag, so enable it after construction
        AutoRefundHandler.isEnabled = true

        viewModel.onUpiWindowEnded()
        advanceUntilIdle()

        assertEquals("txn_1", fetchStatusRepo.lastAutoRefundTransactionId)
    }

    // ── AutoRefundHandler.isEnabled follows configs.autoRefundEnabled ────────

    @Test
    fun `main checkout - loadSession enables auto refund when the session config says so`() = runTest {
        setToken()
        mainScreenRepo.sessionDetailsResult =
            ApiResponse.Success(buildMinimalSessionDetails(autoRefundEnabled = true), responseCode = 200)

        mainViewModel()

        assertTrue(AutoRefundHandler.isEnabled)
    }

    @Test
    fun `main checkout - loadSession leaves auto refund off when the config is false or missing`() = runTest {
        setToken()
        mainScreenRepo.sessionDetailsResult =
            ApiResponse.Success(buildMinimalSessionDetails(autoRefundEnabled = false), responseCode = 200)

        mainViewModel()

        assertFalse(AutoRefundHandler.isEnabled)
    }

    @Test
    fun `main checkout - a new session resets auto refund when its token is empty`() = runTest {
        setToken()
        mainScreenRepo.sessionDetailsResult =
            ApiResponse.Success(buildMinimalSessionDetails(autoRefundEnabled = true), responseCode = 200)
        mainViewModel()
        assertTrue(AutoRefundHandler.isEnabled)

        CheckoutDetailsHandler.resetToDefault() // next session starts without a token
        mainViewModel()

        assertFalse(AutoRefundHandler.isEnabled)
    }

    @Test
    fun `main checkout - a new session resets auto refund when its session call fails`() = runTest {
        setToken()
        mainScreenRepo.sessionDetailsResult =
            ApiResponse.Success(buildMinimalSessionDetails(autoRefundEnabled = true), responseCode = 200)
        mainViewModel()
        assertTrue(AutoRefundHandler.isEnabled)

        mainScreenRepo.sessionDetailsResult = ApiResponse.Error(message = "Network failure")
        mainViewModel()

        assertFalse(AutoRefundHandler.isEnabled)
    }

    @Test
    fun `elements - loadSession enables auto refund when the session config says so`() = runTest {
        setToken()
        mainScreenRepo.sessionDetailsResult =
            ApiResponse.Success(buildMinimalSessionDetails(autoRefundEnabled = true), responseCode = 200)

        elementsViewModel()

        assertTrue(AutoRefundHandler.isEnabled)
    }

    @Test
    fun `elements - a new session resets auto refund when its token is empty`() = runTest {
        setToken()
        mainScreenRepo.sessionDetailsResult =
            ApiResponse.Success(buildMinimalSessionDetails(autoRefundEnabled = true), responseCode = 200)
        elementsViewModel()
        assertTrue(AutoRefundHandler.isEnabled)

        CheckoutDetailsHandler.resetToDefault()
        elementsViewModel()

        assertFalse(AutoRefundHandler.isEnabled)
    }

    @Test
    fun `elements - a new session resets auto refund when its session call fails`() = runTest {
        setToken()
        mainScreenRepo.sessionDetailsResult =
            ApiResponse.Success(buildMinimalSessionDetails(autoRefundEnabled = true), responseCode = 200)
        elementsViewModel()
        assertTrue(AutoRefundHandler.isEnabled)

        mainScreenRepo.sessionDetailsResult = ApiResponse.Error(message = "Network failure")
        elementsViewModel()

        assertFalse(AutoRefundHandler.isEnabled)
    }
}
