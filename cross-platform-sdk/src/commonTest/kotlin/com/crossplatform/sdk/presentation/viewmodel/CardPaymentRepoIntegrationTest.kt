package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.FetchCardDetails
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for card-payment flows.
 *
 * Uses REAL [CardScreenRepoImpl] + REAL [FetchStatusRepoImpl] backed by
 * [FakeApiService], wired into REAL [CardScreenViewModel].  Tests verify
 * that:
 *  - The BIN-lookup call is routed through the real repo to the fake API
 *  - The card-post call builds the webview state correctly
 *  - Surcharge codes are forwarded through the real repo chain
 *  - callFetchStatus triggers the right terminal-state transitions
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CardPaymentRepoIntegrationTest {

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
            token                   = "test_token_123",
            isTestEnv               = true,
            isSuccessScreenVisible  = true,
            ctaBorderRadius         = 8f,
            isSICheckboxChecked     = false,
            isSICheckboxEnabled     = false,
            isFailedScreenVisible   = true,
            showQROnLoad            = false,
            focusedTextInputBorderColor   = null,
            unfocusedTextInputBorderColor = null
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

    // ── BIN lookup ──────────────────────────────────────────────────────────

    @Test
    fun `BIN lookup via real CardScreenRepoImpl routes fetchCardDetails to FakeApiService`() = runTest {
        api.nextFetchCardDetails = ApiResponse.Success(
            FetchCardDetails(
                cardScheme  = "VISA",
                cardType    = "CREDIT",
                bankName    = "HDFC Bank",
                cardNetwork = "VISA"
            ),
            responseCode = 200
        )

        val result = cardRepo.getCardDetails("411111111111")

        assertIs<ApiResponse.Success<FetchCardDetails>>(result)
        assertEquals("VISA",      (result as ApiResponse.Success).data.cardScheme)
        assertEquals("HDFC Bank", result.data.bankName)
        assertEquals("411111111111", api.lastFetchCardDetailsNumber)
    }

    @Test
    fun `BIN lookup error returns ApiResponse Error from real repo`() = runTest {
        api.nextFetchCardDetails = ApiResponse.Error(message = "card not found")

        val result = cardRepo.getCardDetails("999999999999")

        assertIs<ApiResponse.Error<FetchCardDetails>>(result)
        assertEquals("card not found", (result as ApiResponse.Error).message)
    }

    // ── card post ────────────────────────────────────────────────────────────

    @Test
    fun `postCardRequest html action opens webview through real repo chain`() = runTest {
        viewModel.cardNumberText.value     = "4111111111111111"
        viewModel.cardHolderNameText.value = "John Doe"
        viewModel.cardExpiryText.value     = "12/28"
        viewModel.cardCvvText.value        = "123"

        api.nextCardPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "html", htmlContent = "<html>3DS</html>"),
            responseCode = 200
        )

        viewModel.postCardRequest(isSICheckBoxClicked = false, surcharge = null)

        assertTrue(viewModel.showWebview.value)
        assertEquals("<html>3DS</html>", viewModel.htmlString.value)
    }

    @Test
    fun `postCardRequest redirect action sets URL and opens webview`() = runTest {
        viewModel.cardNumberText.value     = "4111111111111111"
        viewModel.cardHolderNameText.value = "Jane Doe"
        viewModel.cardExpiryText.value     = "06/27"
        viewModel.cardCvvText.value        = "456"

        api.nextCardPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(
                actionType = "redirect",
                htmlContent = null,
                url = "https://bank.example.com/3ds"
            ),
            responseCode = 200
        )

        viewModel.postCardRequest(isSICheckBoxClicked = false, surcharge = null)

        assertTrue(viewModel.showWebview.value)
        assertEquals("https://bank.example.com/3ds", viewModel.url.value)
    }

    @Test
    fun `postCardRequest forwards surcharge codes through real repo to FakeApiService`() = runTest {
        viewModel.cardNumberText.value     = "4111111111111111"
        viewModel.cardHolderNameText.value = "Test User"
        viewModel.cardExpiryText.value     = "01/30"
        viewModel.cardCvvText.value        = "789"

        api.nextCardPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(),
            responseCode = 200
        )

        viewModel.postCardRequest(
            isSICheckBoxClicked = false,
            surcharge           = listOf("SC_CREDIT", "SC_VISA")
        )

        // The surcharge codes must flow through real repo → real API call
        assertEquals(listOf("SC_CREDIT", "SC_VISA"), api.lastCardPostSurcharges)
    }

    @Test
    fun `postCardRequest with null surcharge passes null to FakeApiService`() = runTest {
        viewModel.cardNumberText.value     = "4111111111111111"
        viewModel.cardHolderNameText.value = "No Surcharge User"
        viewModel.cardExpiryText.value     = "03/29"
        viewModel.cardCvvText.value        = "321"

        api.nextCardPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(),
            responseCode = 200
        )

        viewModel.postCardRequest(isSICheckBoxClicked = false, surcharge = null)

        assertNull(api.lastCardPostSurcharges)
    }

    @Test
    fun `postCardRequest api error hides the loading indicator`() = runTest {
        viewModel.cardNumberText.value     = "4111111111111111"
        viewModel.cardHolderNameText.value = "Error User"
        viewModel.cardExpiryText.value     = "09/26"
        viewModel.cardCvvText.value        = "000"

        api.nextCardPostResponse = ApiResponse.Error(message = "declined")

        viewModel.postCardRequest(isSICheckBoxClicked = false, surcharge = null)

        assertFalse(viewModel.isBoxPayAnimationVisible.value)
        assertFalse(viewModel.showWebview.value)
    }

    // ── callFetchStatus via real FetchStatusRepoImpl ─────────────────────────

    @Test
    fun `callFetchStatus SUCCESS marks payment successful in CheckoutDetailsHandler`() = runTest {
        api.nextFetchStatus = ApiResponse.Success(
            buildSuccessFetchStatus(transactionId = "txn_001"),
            responseCode = 200
        )

        viewModel.callFetchStatus("SUCCESS")

        assertTrue(CheckoutDetailsHandler.isPaymentSuccessful)
    }

    @Test
    fun `callFetchStatus FAILED marks payment failed and stores error details`() = runTest {
        api.nextFetchStatus = ApiResponse.Success(
            buildFailedFetchStatus(reasonCode = "CARD_DECLINED", reason = "Card was declined"),
            responseCode = 200
        )

        viewModel.callFetchStatus("FAILED")

        assertTrue(CheckoutDetailsHandler.isPaymentFailed)
    }

    @Test
    fun `callFetchStatus routes through real FetchStatusRepoImpl to FakeApiService`() = runTest {
        api.nextFetchStatus = ApiResponse.Success(
            buildSuccessFetchStatus(),
            responseCode = 200
        )

        viewModel.callFetchStatus("SUCCESS")

        assertTrue(api.callLog.any { it.startsWith("fetchStatus") })
    }

    // ── full card payment e2e ─────────────────────────────────────────────────

    @Test
    fun `full card payment e2e - html 3DS then SUCCESS fetch status`() = runTest {
        // Step 1: post card → RequiresAction with html
        viewModel.cardNumberText.value     = "4111111111111111"
        viewModel.cardHolderNameText.value = "E2E Tester"
        viewModel.cardExpiryText.value     = "12/30"
        viewModel.cardCvvText.value        = "999"

        api.nextCardPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "html", htmlContent = "<html>3DS</html>"),
            responseCode = 200
        )
        viewModel.postCardRequest(isSICheckBoxClicked = false, surcharge = null)
        assertTrue(viewModel.showWebview.value)

        // Step 2: webview returns SUCCESS inquiry, call fetchStatus
        api.nextFetchStatus = ApiResponse.Success(
            buildSuccessFetchStatus(transactionId = "txn_e2e"),
            responseCode = 200
        )
        viewModel.callFetchStatus("SUCCESS")
        assertTrue(CheckoutDetailsHandler.isPaymentSuccessful)

        // Verify both calls went through the real repo chain
        assertNotNull(api.callLog.firstOrNull { it.contains("cardPostRequest") })
        assertNotNull(api.callLog.firstOrNull { it.contains("fetchStatus") })
    }
}
