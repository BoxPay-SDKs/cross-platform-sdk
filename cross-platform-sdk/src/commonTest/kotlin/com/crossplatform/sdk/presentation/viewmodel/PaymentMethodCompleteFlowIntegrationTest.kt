package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.Method
import com.crossplatform.sdk.data.model.PaymentActions
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.TransactionStatus
import com.crossplatform.sdk.fakes.FakeCallUIAnalyticsRepo
import com.crossplatform.sdk.fakes.FakeCardScreenRepo
import com.crossplatform.sdk.fakes.FakeFetchStatusRepo
import com.crossplatform.sdk.fakes.FakeInstantOfferRepo
import com.crossplatform.sdk.fakes.FakeMainScreenRepo
import com.crossplatform.sdk.fakes.FakeOtherPaymentMethodRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Complete, end-to-end integration coverage for every payment method the SDK
 * supports, picking up exactly where the existing per-ViewModel tests leave
 * off.
 *
 * The existing suites ([CardScreenViewModelTest], [WalletViewModelTest],
 * [NetBankingViewModelTest], [BNPLViewModelTest], [EMIScreenViewModelTest],
 * [BoxPayElementsViewModelTest]) already prove that `post*Request()` opens
 * the webview for a `RequiresAction` response. None of them go further than
 * that. This file drives the *rest* of the real production flow, exactly as
 * each screen wires it (see `CardScreen.kt`, `WalletScreen.kt`,
 * `NetBankingScreen.kt`, `BNPLScreen.kt`, `EMIScreen.kt`, `MainScreen.kt`):
 *
 *   1. token received                 -> [CheckoutDetailsHandler.setCheckoutToken]
 *   2. user selects a method & submits -> `post*Request()`
 *   3. webview opens for a challenge   -> `showWebview` / `showWebViewScreen`
 *      AND `CheckoutDetailsHandler.checkoutDetails.isWebViewVisible` become true
 *   4. user completes the challenge in the webview and it reports back      ->
 *      `viewModel.callFetchStatus(result)` then `viewModel.setWebViewScreen(false)`
 *      (this is *literally* the callback every WebViewScreen call site uses)
 *   5. the success / failed / expired popup becomes visible                ->
 *      driven purely by [CheckoutDetailsHandler.isPaymentSuccessfulFlow] /
 *      `isPaymentFailedFlow` / `isSessionExpiredFlow` (see `AppNavHost.kt`)
 *
 * Every scenario below asserts step 3 (webview opened) only briefly before
 * moving straight to the part that isn't covered elsewhere: what happens
 * once the webview reports back. Terminal outcomes (success / failed /
 * expired / instant-approval / auto-retry) are spread across different
 * payment methods rather than repeated for all of them, so this file adds
 * real coverage instead of restating the same assertion nine times.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentMethodCompleteFlowIntegrationTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        // isPaymentSuccessful / isPaymentFailed / isSessionExpired are *toggle*
        // flags on CheckoutDetailsHandler (setSessionSuccess() etc. flip
        // `!current`, they don't set `true` outright) - see CheckoutDetailsHandler.kt.
        // Resetting to a known default before every test is what makes a plain
        // `assertTrue(...)` afterwards meaningful instead of leftover state from
        // a previous test.
        CheckoutDetailsHandler.resetToDefault()

        // Deliberately NOT calling setCheckoutToken() here. MainScreenViewModel's
        // init{} calls loadSession() unconditionally, and loadSession() only
        // short-circuits safely when the token is still empty (see its own
        // guard clause); if a real token were already set at construction
        // time, every MainScreenViewModel created below would eagerly kick
        // off a real loadSession() - including loadCountryData(), which reads
        // a Compose resource file - before the test gets a chance to stub
        // anything (this is the same risk MainScreenScreenTest's own doc
        // comment calls out). Each test below constructs its ViewModel(s)
        // first, then calls [receiveCheckoutToken] to represent "the token
        // arrived", exactly mirroring how BoxPayElementsViewModelTest does it.
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        CheckoutDetailsHandler.resetToDefault()
    }

    /** "BoxPayCheckout got the token" - the same call `BoxPayCommonCheckout` makes before any screen renders. */
    private fun receiveCheckoutToken(token: String = "test_token") {
        CheckoutDetailsHandler.setCheckoutToken(
            shopperToken = null,
            token = token,
            isTestEnv = true,
            isSuccessScreenVisible = true,
            ctaBorderRadius = 8,
            isSICheckboxChecked = false,
            isSICheckboxEnabled = false,
            isFailedScreenVisible = true,
            showQROnLoad = false,
            focusedTextInputBorderColor = "#CCCCCC",
            unfocusedTextInputBorderColor = "#DDDDDD",
        )
    }

    // ═══════════════════════════════ CARD ══════════════════════════════════

    @Test
    fun `card - full flow from webview 3DS challenge to the success popup becoming visible`() = runTest {
        val cardRepo = FakeCardScreenRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = CardScreenViewModel(cardRepo, fetchStatusRepo, FakeCallUIAnalyticsRepo())
        receiveCheckoutToken()
        cardRepo.postCardDetailsResult = requiresActionResponse(htmlAction(), brand = "VISA")

        // user clicked Card, entered valid details (covered by
        // CardScreenViewModelTest's own validation tests) and submitted
        viewModel.postCardRequest(isSICheckBoxClicked = false)
        assertTrue(viewModel.showWebview.value, "a RequiresAction/html response must open the webview")
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isWebViewVisible)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful, "must not be visible before the challenge is completed")

        // user completes the 3DS challenge; the webview reports back exactly
        // the way CardScreen.kt's WebViewScreen(onResult) callback does
        fetchStatusRepo.fetchStatusResult = successFetchStatus(brand = "VISA")
        viewModel.callFetchStatus(inquiryResult = "redirect_result_token")
        viewModel.setWebViewScreen(false)

        assertFalse(viewModel.showWebview.value, "webview should close once a terminal status is reached")
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isWebViewVisible)
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful, "success popup is driven by isPaymentSuccessfulFlow")
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isSessionExpired)
        assertEquals("SUCCESS", CheckoutDetailsHandler.checkoutDetails.status)
        assertEquals("VISA", CheckoutDetailsHandler.checkoutDetails.selectedPaymentMethod)
        assertFalse(viewModel.isBoxPayAnimationVisible.value)
    }

    @Test
    fun `card - a decline reported by the webview shows the failed popup with the resolved reason, not the success popup`() = runTest {
        val cardRepo = FakeCardScreenRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = CardScreenViewModel(cardRepo, fetchStatusRepo, FakeCallUIAnalyticsRepo())
        receiveCheckoutToken()
        cardRepo.postCardDetailsResult = requiresActionResponse(htmlAction(), brand = "VISA")
        viewModel.postCardRequest(isSICheckBoxClicked = false)

        fetchStatusRepo.fetchStatusResult = failedFetchStatus(reasonCode = "UF001", reason = "UF001: Card declined by issuing bank")
        viewModel.callFetchStatus(inquiryResult = "redirect_result_token")
        viewModel.setWebViewScreen(false)

        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertEquals("Card declined by issuing bank", CheckoutDetailsHandler.checkoutDetails.errorMessage)
        assertFalse(viewModel.showWebview.value)
    }

    @Test
    fun `card - the webview reporting a stale, expired session shows the expired popup`() = runTest {
        val cardRepo = FakeCardScreenRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = CardScreenViewModel(cardRepo, fetchStatusRepo, FakeCallUIAnalyticsRepo())
        receiveCheckoutToken()
        cardRepo.postCardDetailsResult = requiresActionResponse(redirectAction("https://pay.example.com/card-3ds"), brand = "VISA")
        viewModel.postCardRequest(isSICheckBoxClicked = false)
        assertEquals("https://pay.example.com/card-3ds", viewModel.url.value)

        fetchStatusRepo.fetchStatusResult = expiredFetchStatus()
        viewModel.callFetchStatus(inquiryResult = "")
        viewModel.setWebViewScreen(false)

        assertTrue(CheckoutDetailsHandler.checkoutDetails.isSessionExpired)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed)
    }

    @Test
    fun `card - an immediate Failed status on the post response fails without ever opening a webview`() = runTest {
        val cardRepo = FakeCardScreenRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = CardScreenViewModel(cardRepo, fetchStatusRepo, FakeCallUIAnalyticsRepo())
        receiveCheckoutToken()
        cardRepo.postCardDetailsResult = directTerminalResponse(
            status = "Failed", reasonCode = "UF009", reason = "UF009: Insufficient funds", brand = "VISA",
        )

        viewModel.postCardRequest(isSICheckBoxClicked = false)

        assertFalse(viewModel.showWebview.value, "a direct Failed status has no action to open a webview for")
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed)
        assertEquals("Insufficient funds", CheckoutDetailsHandler.checkoutDetails.errorMessage)
        assertFalse(viewModel.isBoxPayAnimationVisible.value)
    }

    @Test
    fun `card - a retryable failure surfaces the auto-retry option, and retrying successfully shows the success popup`() = runTest {
        val cardRepo = FakeCardScreenRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = CardScreenViewModel(cardRepo, fetchStatusRepo, FakeCallUIAnalyticsRepo())
        receiveCheckoutToken()
        cardRepo.postCardDetailsResult = requiresActionResponse(htmlAction(), brand = "VISA")
        viewModel.postCardRequest(isSICheckBoxClicked = false)

        // webview reports back a non-terminal, retryable status
        fetchStatusRepo.fetchStatusResult = pendingRetryableFetchStatus()
        viewModel.callFetchStatus(inquiryResult = "")

        assertTrue(CheckoutDetailsHandler.checkoutDetails.showRetryBottomDown, "retryable failures must surface the retry drop-down")
        assertFalse(viewModel.isBoxPayAnimationVisible.value)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed, "not a terminal failure yet - retry is still offered")

        // user taps "Retry" on the drop-down
        fetchStatusRepo.autoRetryResult = requiresActionResponse(htmlAction("<html>retry challenge</html>"), brand = "VISA")
        CheckoutDetailsHandler.checkoutDetails.proceedAutoRetryPayment()
        assertTrue(viewModel.showWebview.value, "retrying re-opens the webview for the new challenge")

        // retry succeeds
        fetchStatusRepo.fetchStatusResult = successFetchStatus(brand = "VISA")
        viewModel.callFetchStatus(inquiryResult = "")
        viewModel.setWebViewScreen(false)

        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
    }

    // ═══════════════════════════════ UPI INTENT ═════════════════════════════

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun `upi intent - decodes the app redirect url, and returning to the app after paying shows the success popup`() = runTest {
        val mainScreenRepo = FakeMainScreenRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = MainScreenViewModel(mainScreenRepo, FakeCallUIAnalyticsRepo(), FakeOtherPaymentMethodRepo(), FakeInstantOfferRepo(), fetchStatusRepo)
        receiveCheckoutToken()
        val deepLink = "upi://pay?pa=merchant@okhdfcbank&am=499.00&tr=txn_1"
        val encodedDeepLink = Base64.encode(deepLink.encodeToByteArray())
        mainScreenRepo.upiIntentResult = requiresActionResponse(appRedirectAction(encodedDeepLink), brand = "GPay")

        // user taps "Google Pay" among the UPI intent apps
        viewModel.postUpiIntentRequest(selectedIntent = "gpay", type = "upi/intent")

        assertEquals(deepLink, viewModel.upiIntentUrl.value, "the base64 app-redirect url must be decoded for the OS to launch")

        // user completes the payment in the UPI app and returns to the SDK -
        // this is exactly what AppLifecycleObserver's Foreground callback does
        fetchStatusRepo.fetchStatusResult = successFetchStatus(brand = "GPay")
        viewModel.callFetchStatus(inquiryResult = "")

        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertEquals("GPay", CheckoutDetailsHandler.checkoutDetails.selectedPaymentMethod)
    }

    // ═══════════════════════════════ UPI COLLECT ════════════════════════════

    @Test
    fun `upi collect - a vpa request with no immediate action proceeds to the timer, and a successful status poll shows the success popup`() = runTest {
        val mainScreenRepo = FakeMainScreenRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = MainScreenViewModel(mainScreenRepo, FakeCallUIAnalyticsRepo(), FakeOtherPaymentMethodRepo(), FakeInstantOfferRepo(), fetchStatusRepo)
        receiveCheckoutToken()
        mainScreenRepo.upiCollectResult = requiresActionResponse(action = null, brand = "UpiCollect")

        // user entered a vpa and submitted the collect request
        viewModel.postUpiCollectRequest(shopperVpa = "jane@okhdfcbank", type = "upi/collect")

        assertTrue(viewModel.proceedToTimer.value, "no immediate action -> user is sent to the pending/timer screen")
        assertEquals("jane@okhdfcbank", viewModel.upiId.value)

        // timer screen polls fetchStatus periodically; simulate one poll
        // iteration succeeding - the same call startFetchStatusPolling()'s
        // loop body makes
        fetchStatusRepo.fetchStatusResult = successFetchStatus(brand = "UpiCollect")
        viewModel.callUpiCollectFetchStatue(inquiryResult = "")

        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertFalse(viewModel.isBoxPayAnimationLoading.value)
    }

    @Test
    fun `upi collect - a failed status poll shows the failed popup with the resolved reason`() = runTest {
        val mainScreenRepo = FakeMainScreenRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = MainScreenViewModel(mainScreenRepo, FakeCallUIAnalyticsRepo(), FakeOtherPaymentMethodRepo(), FakeInstantOfferRepo(), fetchStatusRepo)
        receiveCheckoutToken()
        mainScreenRepo.upiCollectResult = requiresActionResponse(action = null, brand = "UpiCollect")
        viewModel.postUpiCollectRequest(shopperVpa = "jane@okhdfcbank", type = "upi/collect")

        fetchStatusRepo.fetchStatusResult = failedFetchStatus(reasonCode = "UF003", reason = "UF003: Collect request declined by payer")
        viewModel.callUpiCollectFetchStatue(inquiryResult = "")

        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertEquals("Collect request declined by payer", CheckoutDetailsHandler.checkoutDetails.errorMessage)
    }

    // ═══════════════════════════════ UPI QR ═════════════════════════════════

    @Test
    fun `upi qr - a qrCode action renders the qr and immediately starts status polling, which reports the payer's success back`() = runTest {
        val mainScreenRepo = FakeMainScreenRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = MainScreenViewModel(mainScreenRepo, FakeCallUIAnalyticsRepo(), FakeOtherPaymentMethodRepo(), FakeInstantOfferRepo(), fetchStatusRepo)
        receiveCheckoutToken()
        mainScreenRepo.upiQrResult = requiresActionResponse(qrAction(content = "upi://pay?pa=merchant@okaxis", expirySec = 300), brand = "UpiQr")
        // postUPIQrRequest's onOpenQr callback calls startFetchStatusPolling()
        // itself (unlike UPI collect, where polling is only started by the
        // screen's LaunchedEffect) - so the very first poll tick fires as
        // part of this call, before we get a chance to configure anything
        // else. The status must therefore already be stubbed to "paid" here.
        fetchStatusRepo.fetchStatusResult = successFetchStatus(brand = "UpiQr")

        viewModel.postUPIQrRequest(type = "upi/qr")

        assertEquals("upi://pay?pa=merchant@okaxis", viewModel.qrImage.value)
        assertEquals(300, viewModel.qrTimer.value)
        assertFalse(viewModel.isBoxPayAnimationLoading.value, "the qr itself is the loading indicator once it's rendered")
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful, "the auto-started status poll already picked up the payer's success")
    }

    @Test
    fun `upi qr - if the payer hasn't paid yet, the status poll leaves the qr up instead of failing the flow`() = runTest {
        val mainScreenRepo = FakeMainScreenRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = MainScreenViewModel(mainScreenRepo, FakeCallUIAnalyticsRepo(), FakeOtherPaymentMethodRepo(), FakeInstantOfferRepo(), fetchStatusRepo)
        receiveCheckoutToken()
        mainScreenRepo.upiQrResult = requiresActionResponse(qrAction(content = "upi://pay?pa=merchant@okaxis", expirySec = 300), brand = "UpiQr")
        // Still pending - not yet Success/Failed/Expired - is reported with a
        // non-retryable NOACTION-mapped status, which handleUpiCollectFetchStatus
        // intentionally no-ops on rather than treating as a terminal failure.
        fetchStatusRepo.fetchStatusResult = ApiResponse.Success(
            FetchStatusResponse(
                status = "PENDING", transactionId = "txn_1", reasonCode = "", reason = "",
                transactionTimestampLocale = "", retryable = false,
                paymentMethod = FetchStatusResponse.PaymentMethod(id = "pm_1"),
            ),
            responseCode = 200,
        )

        viewModel.postUPIQrRequest(type = "upi/qr")

        assertEquals("upi://pay?pa=merchant@okaxis", viewModel.qrImage.value, "qr must stay visible while the payer hasn't paid")
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed, "a still-pending poll must not surface as a failure")
    }

    // ═══════════════════════════════ SAVED CARD / SAVED INSTRUMENT ══════════

    @Test
    fun `saved card - instant approval with no challenge shows the success popup without ever opening a webview`() = runTest {
        val mainScreenRepo = FakeMainScreenRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = MainScreenViewModel(mainScreenRepo, FakeCallUIAnalyticsRepo(), FakeOtherPaymentMethodRepo(), FakeInstantOfferRepo(), fetchStatusRepo)
        receiveCheckoutToken()
        mainScreenRepo.savedCardPostResult = directTerminalResponse(status = "Success", brand = "MASTERCARD")

        // user tapped a saved card in the recommended-instruments list
        viewModel.postSavedCardRequest(instrumentRef = "ref_1", isSICheckboxChecked = false)

        assertFalse(viewModel.showWebViewScreen.value)
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertEquals("MASTERCARD", CheckoutDetailsHandler.checkoutDetails.selectedPaymentMethod)
    }

    @Test
    fun `saved card - a step-up 3DS challenge opens the webview, and completing it shows the success popup`() = runTest {
        val mainScreenRepo = FakeMainScreenRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = MainScreenViewModel(mainScreenRepo, FakeCallUIAnalyticsRepo(), FakeOtherPaymentMethodRepo(), FakeInstantOfferRepo(), fetchStatusRepo)
        receiveCheckoutToken()
        mainScreenRepo.savedCardPostResult = requiresActionResponse(htmlAction(), brand = "MASTERCARD")

        viewModel.postSavedCardRequest(instrumentRef = "ref_1", isSICheckboxChecked = false)
        assertTrue(viewModel.showWebViewScreen.value)

        fetchStatusRepo.fetchStatusResult = successFetchStatus(brand = "MASTERCARD")
        viewModel.callFetchStatus(inquiryResult = "")
        viewModel.setWebViewScreen(false)

        assertFalse(viewModel.showWebViewScreen.value)
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
    }

    // ═══════════════════════════════ EXPRESS CHECKOUT: REVOLUT PAY ══════════

    @Test
    fun `revolut pay - an info action captures the order token and return url, and a subsequent success closes out the flow`() = runTest {
        val mainScreenRepo = FakeMainScreenRepo()
        val otherPaymentMethodRepo = FakeOtherPaymentMethodRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = MainScreenViewModel(mainScreenRepo, FakeCallUIAnalyticsRepo(), otherPaymentMethodRepo, FakeInstantOfferRepo(), fetchStatusRepo)
        receiveCheckoutToken()
        otherPaymentMethodRepo.initiatePaymentResult = ApiResponse.Success(
            PaymentMethodPostResponse(
                transactionId = "txn_1",
                transactionTimestampLocale = "",
                status = TransactionStatus(status = "RequiresAction", reason = "", reasonCode = ""),
                actions = listOf(
                    PaymentActions(type = "info", token = "revolut_order_tok"),
                    PaymentActions(type = "redirect", url = "https://revolut.example.com/return"),
                ),
                paymentMethod = Method(type = "Wallet", brand = "RevolutPay"),
            ),
            responseCode = 200,
        )

        viewModel.onClickRevolutPay()

        assertEquals("revolut_order_tok", viewModel.revolutOrderToken.value)
        assertEquals("https://revolut.example.com/return", viewModel.revolutReturnUrl.value)

        fetchStatusRepo.fetchStatusResult = successFetchStatus(brand = "RevolutPay")
        viewModel.callFetchStatus(inquiryResult = "")

        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
    }

    // ═══════════════════════════════ EXPRESS CHECKOUT: GOOGLE PAY ═══════════

    @Test
    fun `google pay - a redirect action opens the webview, and completing it shows the success popup`() = runTest {
        val mainScreenRepo = FakeMainScreenRepo()
        val otherPaymentMethodRepo = FakeOtherPaymentMethodRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = MainScreenViewModel(mainScreenRepo, FakeCallUIAnalyticsRepo(), otherPaymentMethodRepo, FakeInstantOfferRepo(), fetchStatusRepo)
        receiveCheckoutToken()
        otherPaymentMethodRepo.initiatePaymentResult = requiresActionResponse(redirectAction("https://pay.example.com/googlepay-3ds"), brand = "GooglePay")

        viewModel.onProceedGooglePay(googlePayToken = "gpay_tok_1")

        assertEquals("https://pay.example.com/googlepay-3ds", viewModel.setWebViewUrl.value)
        assertTrue(viewModel.showWebViewScreen.value)

        fetchStatusRepo.fetchStatusResult = successFetchStatus(brand = "GooglePay")
        viewModel.callFetchStatus(inquiryResult = "")
        viewModel.setWebViewScreen(false)

        assertFalse(viewModel.showWebViewScreen.value)
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
    }

    // ═══════════════════════════════ NETBANKING ═════════════════════════════

    @Test
    fun `netbanking - full flow from webview redirect to the success popup becoming visible`() = runTest {
        val repo = FakeOtherPaymentMethodRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = NetBankingViewModel(repo, FakeCallUIAnalyticsRepo(), fetchStatusRepo)
        receiveCheckoutToken()
        repo.initiatePaymentResult = requiresActionResponse(redirectAction("https://pay.example.com/netbanking"), brand = "HDFC")

        viewModel.postNetBankingRequest(instrumentValue = "HDFC")
        assertTrue(viewModel.showWebview.value)
        assertEquals("https://pay.example.com/netbanking", viewModel.url.value)

        fetchStatusRepo.fetchStatusResult = successFetchStatus(brand = "HDFC")
        viewModel.callFetchStatus(inquiryResult = "")
        viewModel.setWebViewScreen(false)

        assertFalse(viewModel.showWebview.value)
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertEquals("HDFC", CheckoutDetailsHandler.checkoutDetails.selectedPaymentMethod)
    }

    // ═══════════════════════════════ WALLET ═════════════════════════════════

    @Test
    fun `wallet - full flow from webview redirect to the success popup becoming visible`() = runTest {
        val repo = FakeOtherPaymentMethodRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = WalletViewModel(repo, FakeCallUIAnalyticsRepo(), fetchStatusRepo)
        receiveCheckoutToken()
        repo.initiatePaymentResult = requiresActionResponse(htmlAction(), brand = "PhonePe")

        viewModel.postWalletRequest(instrumentValue = "PhonePe")
        assertTrue(viewModel.showWebview.value)

        fetchStatusRepo.fetchStatusResult = successFetchStatus(brand = "PhonePe")
        viewModel.callFetchStatus(inquiryResult = "")
        viewModel.setWebViewScreen(false)

        assertFalse(viewModel.showWebview.value)
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
    }

    @Test
    fun `wallet - a declined payment reported by the webview shows the failed popup with the resolved error message`() = runTest {
        val repo = FakeOtherPaymentMethodRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = WalletViewModel(repo, FakeCallUIAnalyticsRepo(), fetchStatusRepo)
        receiveCheckoutToken()
        repo.initiatePaymentResult = requiresActionResponse(htmlAction(), brand = "PhonePe")
        viewModel.postWalletRequest(instrumentValue = "PhonePe")

        fetchStatusRepo.fetchStatusResult = failedFetchStatus(reasonCode = "UF002", reason = "UF002: Wallet balance insufficient")
        viewModel.callFetchStatus(inquiryResult = "")
        viewModel.setWebViewScreen(false)

        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertEquals("Wallet balance insufficient", CheckoutDetailsHandler.checkoutDetails.errorMessage)
        assertFalse(viewModel.showWebview.value)
    }

    // ═══════════════════════════════ BUY NOW PAY LATER ══════════════════════

    @Test
    fun `bnpl - full flow from webview redirect to the success popup becoming visible`() = runTest {
        val repo = FakeOtherPaymentMethodRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = BNPLViewModel(repo, fetchStatusRepo, FakeCallUIAnalyticsRepo())
        receiveCheckoutToken()
        repo.initiatePaymentResult = requiresActionResponse(redirectAction("https://pay.example.com/bnpl"), brand = "Simpl")

        viewModel.postBNPLRequest(instrumentValue = "Simpl")
        assertTrue(viewModel.showWebview.value)

        fetchStatusRepo.fetchStatusResult = successFetchStatus(brand = "Simpl")
        viewModel.callFetchStatus(inquiryResult = "")
        viewModel.setWebViewScreen(false)

        assertFalse(viewModel.showWebview.value)
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
    }

    @Test
    fun `bnpl - the webview reporting an expired session shows the expired popup, not success or failed`() = runTest {
        val repo = FakeOtherPaymentMethodRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        val viewModel = BNPLViewModel(repo, fetchStatusRepo, FakeCallUIAnalyticsRepo())
        receiveCheckoutToken()
        repo.initiatePaymentResult = requiresActionResponse(redirectAction("https://pay.example.com/bnpl"), brand = "Simpl")
        viewModel.postBNPLRequest(instrumentValue = "Simpl")

        fetchStatusRepo.fetchStatusResult = expiredFetchStatus()
        viewModel.callFetchStatus(inquiryResult = "")
        viewModel.setWebViewScreen(false)

        assertTrue(CheckoutDetailsHandler.checkoutDetails.isSessionExpired)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed)
    }

    // ═══════════════════════════════ EMI ════════════════════════════════════

    @Test
    fun `emi - full flow from webview 3DS challenge to the success popup becoming visible`() = runTest {
        val otherPaymentMethodRepo = FakeOtherPaymentMethodRepo()
        val cardScreenRepo = FakeCardScreenRepo()
        val fetchStatusRepo = FakeFetchStatusRepo()
        // EMIScreenViewModel's init{} loads the bank/tenure list up front.
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        val viewModel = EMIScreenViewModel(otherPaymentMethodRepo, cardScreenRepo, FakeCallUIAnalyticsRepo(), fetchStatusRepo)
        receiveCheckoutToken()
        otherPaymentMethodRepo.initiateEMIPaymentResult = requiresActionResponse(htmlAction(), brand = "HDFC EMI")

        viewModel.postEMIRequest()
        assertTrue(viewModel.showWebview.value)

        fetchStatusRepo.fetchStatusResult = successFetchStatus(brand = "HDFC EMI")
        viewModel.callFetchStatus(inquiryResult = "")
        viewModel.setWebViewScreen(false)

        assertFalse(viewModel.showWebview.value)
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
    }

    // ══════════════════════════ shared response builders ════════════════════
    //
    // Kept private to this file (rather than added to the shared fakes) since
    // they encode *scenarios* (a RequiresAction/html response, a terminal
    // Failed poll, ...) rather than reusable test doubles.

    private fun requiresActionResponse(action: PaymentActions?, brand: String) = ApiResponse.Success(
        PaymentMethodPostResponse(
            transactionId = "txn_1",
            transactionTimestampLocale = "17 Aug 2026 10:00 AM",
            status = TransactionStatus(status = "RequiresAction", reason = "", reasonCode = ""),
            actions = action?.let { listOf(it) },
            paymentMethod = Method(type = "Card", brand = brand),
        ),
        responseCode = 200,
    )

    private fun directTerminalResponse(status: String, brand: String, reasonCode: String = "", reason: String = "") = ApiResponse.Success(
        PaymentMethodPostResponse(
            transactionId = "txn_1",
            transactionTimestampLocale = "17 Aug 2026 10:00 AM",
            status = TransactionStatus(status = status, reason = reason, reasonCode = reasonCode),
            actions = null,
            paymentMethod = Method(type = "Card", brand = brand),
        ),
        responseCode = 200,
    )

    private fun htmlAction(html: String = "<html>3DS challenge</html>") = PaymentActions(type = "html", htmlPageString = html)

    private fun redirectAction(url: String) = PaymentActions(type = "redirect", url = url)

    private fun appRedirectAction(base64Url: String) = PaymentActions(type = "appRedirect", url = base64Url)

    private fun qrAction(content: String, expirySec: Int) = PaymentActions(type = "qrCode", content = content, expirySec = expirySec)

    private fun successFetchStatus(brand: String) = ApiResponse.Success(
        FetchStatusResponse(
            status = "SUCCESS",
            transactionId = "txn_1",
            reasonCode = "",
            reason = "",
            transactionTimestampLocale = "17 Aug 2026 10:05 AM",
            retryable = false,
            paymentMethod = FetchStatusResponse.PaymentMethod(id = "pm_1", type = "Card", brand = brand),
        ),
        responseCode = 200,
    )

    private fun failedFetchStatus(reasonCode: String, reason: String) = ApiResponse.Success(
        FetchStatusResponse(
            status = "FAILED",
            transactionId = "txn_1",
            reasonCode = reasonCode,
            reason = reason,
            transactionTimestampLocale = "",
            retryable = false,
            paymentMethod = FetchStatusResponse.PaymentMethod(id = "pm_1"),
        ),
        responseCode = 200,
    )

    private fun expiredFetchStatus() = ApiResponse.Success(
        FetchStatusResponse(
            status = "EXPIRED",
            transactionId = "txn_1",
            reasonCode = "",
            reason = "",
            transactionTimestampLocale = "",
            retryable = false,
            paymentMethod = FetchStatusResponse.PaymentMethod(id = "pm_1"),
        ),
        responseCode = 200,
    )

    private fun pendingRetryableFetchStatus() = ApiResponse.Success(
        FetchStatusResponse(
            status = "PENDING",
            transactionId = "txn_1",
            reasonCode = "",
            reason = "",
            transactionTimestampLocale = "",
            retryable = true,
            paymentMethod = FetchStatusResponse.PaymentMethod(id = "pm_1"),
        ),
        responseCode = 200,
    )
}