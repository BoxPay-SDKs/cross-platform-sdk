package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.repo.FetchStatusRepoImpl
import com.crossplatform.sdk.data.repo.MainScreenRepoImpl
import com.crossplatform.sdk.data.repo.OtherPaymentMethodRepoImpl
import com.crossplatform.sdk.fakes.FakeApiService
import com.crossplatform.sdk.fakes.FakeCallUIAnalyticsRepo
import com.crossplatform.sdk.fakes.FakeInstantOfferRepo
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for all three UPI payment variants:
 *  - UPI Collect (VPA entry)
 *  - UPI Intent (app-redirect)
 *  - UPI QR (PayNow QR screen)
 *
 * Uses REAL [MainScreenRepoImpl] / [OtherPaymentMethodRepoImpl] backed by
 * [FakeApiService], wired into REAL [MainScreenViewModel] / [PayNowScreenViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpiPaymentRepoIntegrationTest {

    private lateinit var api: FakeApiService
    private lateinit var mainRepo: MainScreenRepoImpl
    private lateinit var otherRepo: OtherPaymentMethodRepoImpl
    private lateinit var fetchStatusRepo: FetchStatusRepoImpl
    private lateinit var mainVm: MainScreenViewModel
    private lateinit var payNowVm: PayNowScreenViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        CheckoutDetailsHandler.resetToDefault()
        CheckoutDetailsHandler.setCheckoutToken(
            shopperToken            = null,
            token                   = "upi_token",
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
        mainRepo        = MainScreenRepoImpl(apiService = api, ioDispatcher = UnconfinedTestDispatcher())
        otherRepo       = OtherPaymentMethodRepoImpl(apiService = api, ioDispatcher = UnconfinedTestDispatcher())
        fetchStatusRepo = FetchStatusRepoImpl(apiService = api, ioDispatcher = UnconfinedTestDispatcher())

        mainVm = MainScreenViewModel(
            repo                   = mainRepo,
            analyticsRepo          = FakeCallUIAnalyticsRepo(),
            otherPaymentMethodRepo = otherRepo,
            instantOfferRepo       = FakeInstantOfferRepo(),
            fetchStatusRepo        = fetchStatusRepo
        )
        payNowVm = PayNowScreenViewModel(
            repo            = otherRepo,
            analyticsRepo   = FakeCallUIAnalyticsRepo(),
            fetchStatusRepo = fetchStatusRepo
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        CheckoutDetailsHandler.resetToDefault()
    }

    // ── UPI Collect ──────────────────────────────────────────────────────────

    @Test
    fun `UPI collect via real MainScreenRepoImpl routes to upiCollectPostRequest`() = runTest {
        api.nextUpiCollectPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(
                actionType = "redirect",
                url        = "https://upicollect.example.com/pay"
            ),
            responseCode = 200
        )

        mainVm.postUpiCollectRequest(
            shopperVpa    = "user@okaxis",
            type          = "UPI_COLLECT",
            surchargeList = null
        )

        assertTrue(mainVm.showWebViewScreen.value)
        assertEquals("https://upicollect.example.com/pay", mainVm.setWebViewUrl.value)
        // Verify the VPA was forwarded through real repo
        assertEquals("user@okaxis", api.lastUpiCollectVpa)
    }

    @Test
    fun `UPI collect forwards surcharges through real repo`() = runTest {
        api.nextUpiCollectPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "redirect", url = "https://example.com"),
            responseCode = 200
        )

        mainVm.postUpiCollectRequest(
            shopperVpa    = "pay@upi",
            type          = "UPI_COLLECT",
            surchargeList = listOf("SC_UPI")
        )

        assertEquals(listOf("SC_UPI"), api.lastUpiCollectSurcharges)
    }

    @Test
    fun `UPI collect html action sets htmlString and opens webview`() = runTest {
        api.nextUpiCollectPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "html", htmlContent = "<html>upi</html>"),
            responseCode = 200
        )

        mainVm.postUpiCollectRequest(
            shopperVpa    = "user@paytm",
            type          = "UPI_COLLECT",
            surchargeList = null
        )

        assertTrue(mainVm.showWebViewScreen.value)
        assertEquals("<html>upi</html>", mainVm.setWebViewHtml.value)
    }

    @Test
    fun `UPI collect api error does not open webview`() = runTest {
        api.nextUpiCollectPostResponse = ApiResponse.Error(message = "VPA not found")

        mainVm.postUpiCollectRequest(
            shopperVpa    = "bad@vpa",
            type          = "UPI_COLLECT",
            surchargeList = null
        )

        assertFalse(mainVm.showWebViewScreen.value)
    }

    // ── UPI Intent ──────────────────────────────────────────────────────────

    @Test
    fun `UPI intent via real MainScreenRepoImpl routes to upiIntentPostRequest`() = runTest {
        api.nextUpiIntentPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(
                actionType = "upiIntent",
                url        = "upi://pay?pa=merchant@upi&mc=1234"
            ),
            responseCode = 200
        )

        mainVm.postUpiIntentRequest(
            type          = "UPI_INTENT",
            selectedIntent = "GPAY",
            surchargeList = null
        )

        // The intent URL should be stored so the OS can launch the UPI app
        val intentUrl = mainVm.upiIntentUrl.value
        assertTrue(intentUrl.isNotEmpty() || mainVm.showWebViewScreen.value,
            "Either the intent URL or a webview should be set after UPI intent response")
    }

    @Test
    fun `UPI intent forwards surcharge codes through real repo to FakeApiService`() = runTest {
        api.nextUpiIntentPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "redirect", url = "upi://pay"),
            responseCode = 200
        )

        mainVm.postUpiIntentRequest(
            type          = "UPI_INTENT",
            selectedIntent = "PHONEPE",
            surchargeList = listOf("SC_UPI_INTENT")
        )

        assertEquals(listOf("SC_UPI_INTENT"), api.lastUpiIntentSurcharges)
    }

    @Test
    fun `UPI intent null surcharge passes null through real repo`() = runTest {
        api.nextUpiIntentPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "redirect", url = "upi://pay"),
            responseCode = 200
        )

        mainVm.postUpiIntentRequest(type = "UPI_INTENT", selectedIntent = "PAYTM", surchargeList = null)

        assertNull(api.lastUpiIntentSurcharges)
    }

    // ── UPI QR via PayNowScreenViewModel + real OtherPaymentMethodRepoImpl ──

    @Test
    fun `UPI QR via real OtherPaymentMethodRepoImpl routes methodsPostRequest for QR type`() = runTest {
        api.nextMethodsPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(
                actionType = "qrCode",
                qrContent  = "QR_DATA_BASE64",
                expirySec  = 300
            ),
            responseCode = 200
        )

        payNowVm.getPayNowQR(instrumentType = "UPI_QR", surcharge = null)

        // methodsPostRequest should have been called through the real repo
        assertTrue(api.callLog.any { it.contains("methodsPostRequest") })
    }

    @Test
    fun `UPI QR surcharge codes forwarded through real OtherPaymentMethodRepoImpl`() = runTest {
        api.nextMethodsPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "qrCode", qrContent = "QR", expirySec = 120),
            responseCode = 200
        )

        payNowVm.getPayNowQR(instrumentType = "UPI_QR", surcharge = listOf("SC_QR"))

        assertEquals(listOf("SC_QR"), api.lastMethodsPostSurcharges)
    }

    // ── UPI Collect then fetch status e2e ────────────────────────────────────

    @Test
    fun `UPI collect e2e - redirect then SUCCESS fetch status marks payment complete`() = runTest {
        // Step 1: UPI collect → redirect
        api.nextUpiCollectPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "redirect", url = "https://bank.example.com"),
            responseCode = 200
        )
        mainVm.postUpiCollectRequest(
            shopperVpa    = "success@upi",
            type          = "UPI_COLLECT",
            surchargeList = null
        )
        assertTrue(mainVm.showWebViewScreen.value)

        // Step 2: fetchStatus → SUCCESS
        api.nextFetchStatus = ApiResponse.Success(
            buildSuccessFetchStatus(transactionId = "txn_upi_001"),
            responseCode = 200
        )
        mainVm.callFetchStatus("SUCCESS")
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)

        // Verify both calls were routed through real repos
        assertNotNull(api.callLog.firstOrNull { it.contains("upiCollectPostRequest") })
        assertNotNull(api.callLog.firstOrNull { it.contains("fetchStatus") })
    }

    @Test
    fun `UPI collect e2e - redirect then FAILED fetch status marks payment failed`() = runTest {
        api.nextUpiCollectPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "redirect", url = "https://bank.example.com"),
            responseCode = 200
        )
        mainVm.postUpiCollectRequest(
            shopperVpa    = "fail@upi",
            type          = "UPI_COLLECT",
            surchargeList = null
        )

        api.nextFetchStatus = ApiResponse.Success(
            buildFailedFetchStatus(reason = "UPI collect failed"),
            responseCode = 200
        )
        mainVm.callFetchStatus("FAILED")

        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed)
    }
}
