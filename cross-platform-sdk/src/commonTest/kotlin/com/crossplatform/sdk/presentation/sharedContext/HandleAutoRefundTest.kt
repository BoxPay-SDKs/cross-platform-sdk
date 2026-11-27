package com.crossplatform.sdk.presentation.sharedContext

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AutoRefundTimerResponse
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.fakes.FakeFetchStatusRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Auto refund: a PENDING status (with `configs.autoRefundEnabled`) starts the auto-refund
 * timer, polls the status every 4 seconds and fails the payment if it is still PENDING when
 * the timer ends. All timing runs on virtual time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HandleAutoRefundTest {

    private lateinit var repo: FakeFetchStatusRepo
    private val animationCalls = mutableListOf<Boolean>()

    @BeforeTest
    fun setUp() {
        CheckoutDetailsHandler.resetToDefault()
        AutoRefundHandler.resetForTest()
        AutoRefundHandler.isEnabled = true
        repo = FakeFetchStatusRepo()
        animationCalls.clear()
    }

    @AfterTest
    fun tearDown() {
        AutoRefundHandler.resetForTest()
        CheckoutDetailsHandler.resetToDefault()
    }

    private fun status(value: String, transactionId: String = "txn_1") =
        ApiResponse.Success(
            FetchStatusResponse(
                status = value,
                transactionId = transactionId,
                reasonCode = "",
                reason = "",
                transactionTimestampLocale = "01/06/2025 10:00:00",
                retryable = false,
                paymentMethod = FetchStatusResponse.PaymentMethod(id = "pm_1", brand = "VISA"),
            ),
            responseCode = 200,
        )

    private fun timer(seconds: Int) =
        ApiResponse.Success(
            AutoRefundTimerResponse(
                token = "token",
                transactionId = "txn_1",
                timerStarted = true,
                autoRefundAt = "2026-11-23T10:46:12.835551837Z",
                autoRefundTimeoutSeconds = seconds,
            ),
            responseCode = 200,
        )

    private fun TestScope.useVirtualClock() {
        AutoRefundHandler.clock = { testScheduler.currentTime }
    }

    private suspend fun run(isPollingFlow: Boolean = false) = fetchStatusWithAutoRefund(
        fetchStatusRepo = repo,
        setIsBoxPayAnimationVisible = { animationCalls.add(it) },
        onAutoRetry = {},
        isPollingFlow = isPollingFlow,
    )

    @Test
    fun `flag off - pending never calls the auto refund api`() = runTest {
        AutoRefundHandler.isEnabled = false
        repo.fetchStatusResult = status("PENDING")

        run()

        assertNull(repo.lastAutoRefundTransactionId)
        assertEquals(1, repo.fetchStatusCallCount)
        assertNull(AutoRefundHandler.secondsLeft.value)
    }

    @Test
    fun `non pending status is handled as before and never starts the timer`() = runTest {
        repo.fetchStatusResult = status("SUCCESS")

        run()

        assertNull(repo.lastAutoRefundTransactionId)
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed)
    }

    @Test
    fun `pending then success - timer api called with the transaction id and success handled`() = runTest {
        useVirtualClock()
        repo.startAutoRefundTimerResult = timer(30)
        repo.fetchStatusQueue.addAll(listOf(status("PENDING"), status("PENDING"), status("SUCCESS")))

        run()

        assertEquals("txn_1", repo.lastAutoRefundTransactionId)
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed)
        // initial check + polls at 4s and 8s
        assertEquals(3, repo.fetchStatusCallCount)
        assertTrue(testScheduler.currentTime in 8_000L..8_999L, "resolved on the 8s poll, was ${testScheduler.currentTime}")
        assertNull(AutoRefundHandler.secondsLeft.value, "countdown must be cleared")
        assertFalse(AutoRefundHandler.isRunning)
    }

    @Test
    fun `still pending when the timer ends - payment fails with the auto refund message`() = runTest {
        useVirtualClock()
        repo.startAutoRefundTimerResult = timer(10)
        repo.fetchStatusResult = status("PENDING")

        run()

        val details = CheckoutDetailsHandler.checkoutDetails
        assertTrue(details.isPaymentFailed)
        assertFalse(details.isPaymentSuccessful)
        assertTrue(
            details.errorMessage.contains("refunded or released automatically"),
            "unexpected error message: ${details.errorMessage}",
        )
        // initial check + polls at 4s and 8s + one last check when the timer ends
        assertEquals(4, repo.fetchStatusCallCount)
        assertTrue(testScheduler.currentTime in 10_000L..10_999L, "ended at the 10s timeout, was ${testScheduler.currentTime}")
        assertEquals(false, animationCalls.last(), "loader must be hidden after the failure")
        assertNull(AutoRefundHandler.secondsLeft.value)
        assertFalse(AutoRefundHandler.isRunning)
    }

    private fun assertOldBehaviourWithoutCountdown() {
        assertEquals(1, repo.fetchStatusCallCount, "no polling when the server timer did not start")
        assertFalse(
            CheckoutDetailsHandler.checkoutDetails.errorMessage.contains("refunded or released automatically"),
            "must not promise a refund when no server timer exists",
        )
        assertNull(AutoRefundHandler.secondsLeft.value)
        assertFalse(AutoRefundHandler.isRunning)
    }

    @Test
    fun `timer api failure - no countdown and old behaviour`() = runTest {
        useVirtualClock()
        repo.startAutoRefundTimerResult = ApiResponse.Error("boom")
        repo.fetchStatusResult = status("PENDING")

        run()

        assertEquals(0L, testScheduler.currentTime)
        assertOldBehaviourWithoutCountdown()
    }

    @Test
    fun `timerStarted false - no countdown and old behaviour`() = runTest {
        useVirtualClock()
        repo.startAutoRefundTimerResult = ApiResponse.Success(
            AutoRefundTimerResponse(timerStarted = false, autoRefundTimeoutSeconds = 30),
            responseCode = 200,
        )
        repo.fetchStatusResult = status("PENDING")

        run()

        assertEquals(0L, testScheduler.currentTime)
        assertOldBehaviourWithoutCountdown()
    }

    @Test
    fun `missing timeout seconds falls back to the 30 second default`() = runTest {
        useVirtualClock()
        repo.startAutoRefundTimerResult = timer(0)
        repo.fetchStatusResult = status("PENDING")

        run()

        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed)
        assertTrue(testScheduler.currentTime in 30_000L..30_999L, "was ${testScheduler.currentTime}")
    }

    @Test
    fun `countdown is published every second and cleared at the end`() = runTest {
        useVirtualClock()
        repo.startAutoRefundTimerResult = timer(5)
        repo.fetchStatusResult = status("PENDING")
        val seen = mutableListOf<Int?>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            AutoRefundHandler.secondsLeft.collect { seen.add(it) }
        }

        run()
        collector.cancel()

        val counts = seen.filterNotNull()
        assertEquals(5, counts.first())
        assertEquals(0, counts.last())
        assertEquals(counts.sortedDescending(), counts, "countdown must only go down")
        assertNull(seen.last(), "countdown must be cleared when the flow ends")
    }

    @Test
    fun `a second call is ignored while a countdown is running`() = runTest {
        AutoRefundHandler.isRunning = true
        repo.fetchStatusResult = status("PENDING")

        run()

        assertEquals(0, repo.fetchStatusCallCount)
        assertNull(repo.lastAutoRefundTransactionId)
    }

    @Test
    fun `polling flow - terminal status handled by the collect handler`() = runTest {
        repo.fetchStatusResult = status("SUCCESS")

        run(isPollingFlow = true)

        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
    }

    @Test
    fun `polling flow with flag off - pending stays a no-op and does not fail the payment`() = runTest {
        AutoRefundHandler.isEnabled = false
        repo.fetchStatusResult = status("PENDING")

        run(isPollingFlow = true)

        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertNull(repo.lastAutoRefundTransactionId)
    }
}
