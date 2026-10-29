package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.Method
import com.crossplatform.sdk.data.model.PaymentActions
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.TransactionStatus
import com.crossplatform.sdk.domain.model.PayNowUiState
import com.crossplatform.sdk.domain.model.SurchargeModel
import com.crossplatform.sdk.fakes.FakeCallUIAnalyticsRepo
import com.crossplatform.sdk.fakes.FakeFetchStatusRepo
import com.crossplatform.sdk.fakes.FakeOtherPaymentMethodRepo
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers [PayNowScreenViewModel]:
 *  1. [getPayNowQR] — QR-code path, webview-html path, webview-redirect path, error path.
 *  2. [markExpired] / [setExpiredQRState] — state transitions.
 *  3. Polling lifecycle — [startFetchStatusPolling], [pausePolling], [resumePolling],
 *     [stopFetchStatusPolling].
 *  4. [autoRetryInitiatePayment] — html and error outcomes.
 *  5. [resolveSurchargeList] — the pure filtering function.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayNowScreenViewModelTest {

    private lateinit var repo: FakeOtherPaymentMethodRepo
    private lateinit var analyticsRepo: FakeCallUIAnalyticsRepo
    private lateinit var fetchStatusRepo: FakeFetchStatusRepo
    private lateinit var viewModel: PayNowScreenViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = FakeOtherPaymentMethodRepo()
        analyticsRepo = FakeCallUIAnalyticsRepo()
        fetchStatusRepo = FakeFetchStatusRepo()
        viewModel = PayNowScreenViewModel(repo, analyticsRepo, fetchStatusRepo)
        CheckoutDetailsHandler.resetToDefault()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        CheckoutDetailsHandler.resetToDefault()
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private fun requiresActionResponse(action: PaymentActions) = ApiResponse.Success(
        PaymentMethodPostResponse(
            transactionId = "txn_1",
            transactionTimestampLocale = "17 Aug 2026",
            status = TransactionStatus(status = "RequiresAction", reason = "", reasonCode = ""),
            actions = listOf(action),
            paymentMethod = Method(type = "Upi", brand = "UPI_QR"),
        ),
        responseCode = 200,
    )

    private fun errorResponse(): ApiResponse<PaymentMethodPostResponse> = ApiResponse.Error(message = "network error")

    private fun surchargeModel(
        applicableOn: String,
        surchargeCode: String,
        network: String = "",
    ) = SurchargeModel(
        applicableOn = applicableOn,
        title = "Surcharge",
        surchargeCode = surchargeCode,
        network = network,
        classification = "CONSUMER",
        amount = 10.0,
        description = "",
    )

    // ── getPayNowQR – QR path ────────────────────────────────────────────────

    @Test
    fun `getPayNowQR with a qrCode action sets qrState to Ready and stops the fetching indicator`() = runTest {
        repo.initiatePaymentResult = requiresActionResponse(
            PaymentActions(type = "qrCode", content = "UPI123", expirySec = 120)
        )

        viewModel.getPayNowQR(instrumentType = "UPI_QR", surcharge = null)

        val state = viewModel.qrState.value
        assertIs<PayNowUiState.Ready>(state)
        assertEquals("UPI123", state.qrImage)
        assertEquals(120, state.totalSeconds)
        assertFalse(viewModel.isQRFetching.value)
    }

    @Test
    fun `getPayNowQR with an html action opens the webview and does not update qrState`() = runTest {
        repo.initiatePaymentResult = requiresActionResponse(
            PaymentActions(type = "html", htmlPageString = "<html>pay</html>")
        )

        viewModel.getPayNowQR(instrumentType = "UPI_QR", surcharge = null)

        assertTrue(viewModel.showWebview.value)
        assertEquals("<html>pay</html>", viewModel.htmlString.value)
        assertNull(viewModel.qrState.value)
    }

    @Test
    fun `getPayNowQR with a redirect action stores the url and opens the webview`() = runTest {
        repo.initiatePaymentResult = requiresActionResponse(
            PaymentActions(type = "redirect", url = "https://pay.example.com/qr")
        )

        viewModel.getPayNowQR(instrumentType = "UPI_QR", surcharge = null)

        assertTrue(viewModel.showWebview.value)
        assertEquals("https://pay.example.com/qr", viewModel.url.value)
    }

    @Test
    fun `getPayNowQR with an api error stops the fetching indicator`() = runTest {
        repo.initiatePaymentResult = errorResponse()

        viewModel.getPayNowQR(instrumentType = "UPI_QR", surcharge = null)

        assertFalse(viewModel.isQRFetching.value)
    }

    @Test
    fun `getPayNowQR fires the PAYMENT_INITIATED analytics event`() = runTest {
        repo.initiatePaymentResult = errorResponse()

        viewModel.getPayNowQR(instrumentType = "UPI_QR", surcharge = null)

        assertTrue(analyticsRepo.events.isNotEmpty())
        assertTrue(analyticsRepo.events.any { it.first.contains("PAYMENT_INITIATED", ignoreCase = true) })
    }

    // ── markExpired / setExpiredQRState ─────────────────────────────────────

    @Test
    fun `markExpired transitions qrState to Expired`() {
        viewModel.markExpired()

        assertEquals(PayNowUiState.Expired, viewModel.qrState.value)
    }

    @Test
    fun `setExpiredQRState also transitions qrState to Expired`() {
        viewModel.setExpiredQRState()

        assertEquals(PayNowUiState.Expired, viewModel.qrState.value)
    }

    // ── polling lifecycle ────────────────────────────────────────────────────

    @Test
    fun `pausePolling stops an active polling job`() = runTest {
        // With an UnconfinedTestDispatcher, startFetchStatusPolling's own
        // while(isActive) loop already runs its first iteration (one
        // callFetchStatus call) synchronously before control returns here —
        // so the baseline is whatever that kick-off already did, not zero.
        viewModel.startFetchStatusPolling()
        val countAfterStart = fetchStatusRepo.fetchStatusCallCount

        viewModel.pausePolling()

        // After a pause, resumePolling should not restart polling because
        // qrState is still null (not Ready), so the call count must not grow.
        viewModel.resumePolling()
        assertEquals(countAfterStart, fetchStatusRepo.fetchStatusCallCount)
    }

    @Test
    fun `stopFetchStatusPolling clears the polling-intended flag so resumePolling is a no-op`() = runTest {
        // Put the VM into a "polling was running" state by simulating a Ready QR.
        repo.initiatePaymentResult = requiresActionResponse(
            PaymentActions(type = "qrCode", content = "X", expirySec = 60)
        )
        fetchStatusRepo.fetchStatusResult = ApiResponse.Loading // prevent actual calls completing
        viewModel.getPayNowQR(instrumentType = "UPI_QR", surcharge = null)

        viewModel.stopFetchStatusPolling()
        // resumePolling must be a no-op since isPollingIntended is now false.
        viewModel.resumePolling()

        // fetchStatus call count may be 1 from the initial polling kick-off in
        // startFetchStatusPolling() inside getPayNowQR, but it should not grow
        // further after stop+resume.
        val countAfterStop = fetchStatusRepo.fetchStatusCallCount
        viewModel.resumePolling()
        assertEquals(countAfterStop, fetchStatusRepo.fetchStatusCallCount)
    }

    // ── autoRetryInitiatePayment ─────────────────────────────────────────────

    @Test
    fun `autoRetryInitiatePayment with an html action opens the webview`() = runTest {
        fetchStatusRepo.autoRetryResult = ApiResponse.Success(
            PaymentMethodPostResponse(
                transactionId = "txn_2",
                transactionTimestampLocale = "17 Aug 2026",
                status = TransactionStatus(status = "RequiresAction", reason = "", reasonCode = ""),
                actions = listOf(PaymentActions(type = "html", htmlPageString = "<html>retry</html>")),
                paymentMethod = Method(type = "Upi", brand = "UPI_QR"),
            ),
            responseCode = 200,
        )

        viewModel.autoRetryInitiatePayment()

        assertTrue(viewModel.showWebview.value)
        assertEquals("<html>retry</html>", viewModel.htmlString.value)
    }

    @Test
    fun `autoRetryInitiatePayment with an api error stops the loading indicator`() = runTest {
        fetchStatusRepo.autoRetryResult = ApiResponse.Error(message = "timeout")

        viewModel.autoRetryInitiatePayment()

        assertFalse(viewModel.isQRFetching.value)
    }

    // ── setWebViewScreen ─────────────────────────────────────────────────────

    @Test
    fun `setWebViewScreen true makes showWebview true and propagates to CheckoutDetailsHandler`() {
        viewModel.setWebViewScreen(true)

        assertTrue(viewModel.showWebview.value)
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isWebViewVisible)
    }

    @Test
    fun `setWebViewScreen false hides the webview and propagates to CheckoutDetailsHandler`() {
        viewModel.setWebViewScreen(true)
        viewModel.setWebViewScreen(false)

        assertFalse(viewModel.showWebview.value)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isWebViewVisible)
    }

    // ── resolveSurchargeList ─────────────────────────────────────────────────

    @Test
    fun `resolveSurchargeList returns null when no surcharges are configured`() {
        CheckoutDetailsHandler.setSurchargeDetails(emptyList())

        val result = viewModel.resolveSurchargeList(selectedMethod = "credit")

        assertNull(result)
    }

    @Test
    fun `resolveSurchargeList returns codes that match the selected method`() {
        CheckoutDetailsHandler.setSurchargeDetails(
            listOf(
                surchargeModel(applicableOn = "credit", surchargeCode = "SC_CREDIT"),
                surchargeModel(applicableOn = "debit",  surchargeCode = "SC_DEBIT"),
            )
        )

        val result = viewModel.resolveSurchargeList(selectedMethod = "credit")

        assertEquals(listOf("SC_CREDIT"), result)
    }

    @Test
    fun `resolveSurchargeList matching is case-insensitive`() {
        CheckoutDetailsHandler.setSurchargeDetails(
            listOf(surchargeModel(applicableOn = "Credit", surchargeCode = "SC1"))
        )

        val result = viewModel.resolveSurchargeList(selectedMethod = "CREDIT")

        assertEquals(listOf("SC1"), result)
    }

    @Test
    fun `resolveSurchargeList filters by network when one is provided`() {
        CheckoutDetailsHandler.setSurchargeDetails(
            listOf(
                surchargeModel(applicableOn = "credit", surchargeCode = "VISA_SC", network = "VISA"),
                surchargeModel(applicableOn = "credit", surchargeCode = "MC_SC",   network = "MasterCard"),
            )
        )

        val result = viewModel.resolveSurchargeList(selectedMethod = "credit", selectedNetwork = "VISA")

        assertEquals(listOf("VISA_SC"), result)
    }

    @Test
    fun `resolveSurchargeList includes surcharges with a blank network for any selected network`() {
        CheckoutDetailsHandler.setSurchargeDetails(
            listOf(surchargeModel(applicableOn = "credit", surchargeCode = "SC_ANY", network = ""))
        )

        val result = viewModel.resolveSurchargeList(selectedMethod = "credit", selectedNetwork = "VISA")

        assertEquals(listOf("SC_ANY"), result)
    }

    @Test
    fun `resolveSurchargeList returns null rather than an empty list when nothing matches`() {
        CheckoutDetailsHandler.setSurchargeDetails(
            listOf(surchargeModel(applicableOn = "debit", surchargeCode = "SC_DEBIT"))
        )

        val result = viewModel.resolveSurchargeList(selectedMethod = "credit")

        assertNull(result)
    }

    @Test
    fun `resolveSurchargeList network comparison ignores spaces`() {
        CheckoutDetailsHandler.setSurchargeDetails(
            listOf(surchargeModel(applicableOn = "credit", surchargeCode = "SC1", network = "Master Card"))
        )

        val result = viewModel.resolveSurchargeList(selectedMethod = "credit", selectedNetwork = "MasterCard")

        assertEquals(listOf("SC1"), result)
    }
}
