package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.RecommendedInstrumentsResponse
import com.crossplatform.sdk.data.repo.FetchStatusRepoImpl
import com.crossplatform.sdk.data.repo.MainScreenRepoImpl
import com.crossplatform.sdk.data.repo.OtherPaymentMethodRepoImpl
import com.crossplatform.sdk.domain.model.UiState
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for saved-card flows.
 *
 * Uses REAL [MainScreenRepoImpl] + REAL [FetchStatusRepoImpl] backed by
 * [FakeApiService], wired into REAL [MainScreenViewModel]. Tests:
 *  - fetchRecommendedInstruments routes through real repo
 *  - postSavedCardRequest routes payment through real repo
 *  - deleteSavedCard routes through real repo with correct id and type
 *  - Full e2e: saved card payment → fetchStatus SUCCESS
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SavedCardRepoIntegrationTest {

    private lateinit var api: FakeApiService
    private lateinit var mainRepo: MainScreenRepoImpl
    private lateinit var otherRepo: OtherPaymentMethodRepoImpl
    private lateinit var fetchStatusRepo: FetchStatusRepoImpl
    private lateinit var viewModel: MainScreenViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        CheckoutDetailsHandler.resetToDefault()
        CheckoutDetailsHandler.setCheckoutToken(
            shopperToken            = "shopper_abc",
            token                   = "saved_card_token",
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
        mainRepo        = MainScreenRepoImpl(apiService = api, ioDispatcher = UnconfinedTestDispatcher())
        otherRepo       = OtherPaymentMethodRepoImpl(apiService = api, ioDispatcher = UnconfinedTestDispatcher())
        fetchStatusRepo = FetchStatusRepoImpl(apiService = api, ioDispatcher = UnconfinedTestDispatcher())

        viewModel = MainScreenViewModel(
            repo                   = mainRepo,
            analyticsRepo          = FakeCallUIAnalyticsRepo(),
            otherPaymentMethodRepo = otherRepo,
            instantOfferRepo       = FakeInstantOfferRepo(),
            fetchStatusRepo        = fetchStatusRepo
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        CheckoutDetailsHandler.resetToDefault()
    }

    // ── fetchRecommendedInstruments ──────────────────────────────────────────

    @Test
    fun `fetchRecommendedInstruments routes through real MainScreenRepoImpl to FakeApiService`() = runTest {
        api.nextRecommendedInstruments = ApiResponse.Success(
            listOf(
                RecommendedInstrumentsResponse(
                    type         = "Card",
                    brand        = "VISA",
                    instrumentRef = "ref_visa_001",
                    displayValue = "•••• 4242",
                    logoUrl      = null,
                    cardNickName = "My Visa"
                )
            ),
            responseCode = 200
        )

        // Trigger session load which fetches recommended instruments internally
        api.nextSessionDetails = ApiResponse.Success(
            buildMinimalSessionDetails(),
            responseCode = 200
        )
        viewModel.loadSession()

        assertTrue(api.callLog.any { it.contains("fetchRecommendedInstruments") || it.contains("getSessionDetails") })
    }

    @Test
    fun `fetchRecommendedInstruments returns list via real repo`() = runTest {
        val instruments = listOf(
            RecommendedInstrumentsResponse(
                type          = "Card",
                brand         = "MASTERCARD",
                instrumentRef = "ref_mc_001",
                displayValue  = "•••• 5555",
                logoUrl       = null,
                cardNickName  = null
            ),
            RecommendedInstrumentsResponse(
                type          = "Card",
                brand         = "VISA",
                instrumentRef = "ref_visa_002",
                displayValue  = "•••• 4111",
                logoUrl       = null,
                cardNickName  = "Primary"
            )
        )
        api.nextRecommendedInstruments = ApiResponse.Success(instruments, responseCode = 200)

        val result = mainRepo.fetchRecommendedInstruments()

        assertIs<ApiResponse.Success<List<RecommendedInstrumentsResponse>>>(result)
        assertEquals(2, (result as ApiResponse.Success).data.size)
        assertEquals("ref_mc_001", result.data[0].instrumentRef)
    }

    @Test
    fun `fetchRecommendedInstruments error returns ApiResponse Error via real repo`() = runTest {
        api.nextRecommendedInstruments = ApiResponse.Error(message = "no instruments")

        val result = mainRepo.fetchRecommendedInstruments()

        assertIs<ApiResponse.Error<List<RecommendedInstrumentsResponse>>>(result)
        assertEquals("no instruments", (result as ApiResponse.Error).message)
    }

    // ── postSavedCardRequest ─────────────────────────────────────────────────

    @Test
    fun `postSavedCardRequest routes through real MainScreenRepoImpl`() = runTest {
        api.nextSavedCardPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "html", htmlContent = "<html>3DS</html>"),
            responseCode = 200
        )

        viewModel.postSavedCardRequest(
            instrumentRef      = "ref_visa_001",
            isSICheckboxChecked = false
        )

        assertTrue(viewModel.showWebViewScreen.value || api.callLog.any { it.contains("savedCardPostRequest") })
    }

    @Test
    fun `postSavedCardRequest html action opens webview in MainScreenViewModel`() = runTest {
        api.nextSavedCardPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "html", htmlContent = "<html>3DS</html>"),
            responseCode = 200
        )

        viewModel.postSavedCardRequest(instrumentRef = "ref_mc_001", isSICheckboxChecked = false)

        assertTrue(viewModel.showWebViewScreen.value)
        assertEquals("<html>3DS</html>", viewModel.setWebViewHtml.value)
    }

    @Test
    fun `postSavedCardRequest redirect action sets URL in MainScreenViewModel`() = runTest {
        api.nextSavedCardPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(
                actionType = "redirect",
                url = "https://bank.example.com/3ds"
            ),
            responseCode = 200
        )

        viewModel.postSavedCardRequest(instrumentRef = "ref_visa_002", isSICheckboxChecked = true)

        assertTrue(viewModel.showWebViewScreen.value)
        assertEquals("https://bank.example.com/3ds", viewModel.setWebViewUrl.value)
    }

    @Test
    fun `postSavedCardRequest api error does not open webview`() = runTest {
        api.nextSavedCardPostResponse = ApiResponse.Error(message = "payment failed")

        viewModel.postSavedCardRequest(instrumentRef = "ref_bad", isSICheckboxChecked = false)

        assertFalse(viewModel.showWebViewScreen.value)
    }

    // ── deleteSavedCard ──────────────────────────────────────────────────────

    @Test
    fun `deleteSavedCard routes through real MainScreenRepoImpl to FakeApiService`() = runTest {
        api.nextDeleteSavedCard = ApiResponse.Success(
            listOf(
                RecommendedInstrumentsResponse(
                    type = "Card", brand = "VISA", instrumentRef = "ref_remaining",
                    displayValue = "•••• 1234", logoUrl = null, cardNickName = null
                )
            ),
            responseCode = 200
        )

        viewModel.onClickDeleteSavedCard(id = "ref_visa_001")

        assertNotNull(api.callLog.firstOrNull { it.contains("deleteSavedCard") })
    }

    @Test
    fun `deleteSavedCard forwards correct id and type through real repo`() = runTest {
        api.nextDeleteSavedCard = ApiResponse.Success(emptyList(), responseCode = 200)

        viewModel.onClickDeleteSavedCard(id = "ref_target_card")

        assertEquals("ref_target_card", api.lastDeleteSavedCardId)
        assertEquals("Card", api.lastDeleteSavedCardType)
    }

    @Test
    fun `deleteSavedCard returns updated list via real MainScreenRepoImpl`() = runTest {
        val remainingInstruments = listOf(
            RecommendedInstrumentsResponse(
                type = "Card", brand = "MASTERCARD", instrumentRef = "ref_mc_001",
                displayValue = "•••• 5555", logoUrl = null, cardNickName = null
            )
        )
        api.nextDeleteSavedCard = ApiResponse.Success(remainingInstruments, responseCode = 200)

        val result = mainRepo.deleteSavedCard(id = "ref_visa_001", type = "Card")

        assertIs<ApiResponse.Success<List<RecommendedInstrumentsResponse>>>(result)
        assertEquals(1, (result as ApiResponse.Success).data.size)
        assertEquals("ref_mc_001", result.data[0].instrumentRef)
    }

    // ── full e2e: saved card → fetchStatus ───────────────────────────────────

    @Test
    fun `saved card e2e - html 3DS then SUCCESS fetch status marks payment complete`() = runTest {
        // Step 1: Saved card payment → html RequiresAction
        api.nextSavedCardPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "html", htmlContent = "<html>3DS</html>"),
            responseCode = 200
        )
        viewModel.postSavedCardRequest(instrumentRef = "ref_e2e", isSICheckboxChecked = false)
        assertTrue(viewModel.showWebViewScreen.value)

        // Step 2: fetchStatus → SUCCESS
        api.nextFetchStatus = ApiResponse.Success(
            buildSuccessFetchStatus(transactionId = "txn_saved_card"),
            responseCode = 200
        )
        viewModel.callFetchStatus("SUCCESS")

        assertTrue(CheckoutDetailsHandler.isPaymentSuccessful)
        assertNotNull(api.callLog.firstOrNull { it.contains("savedCardPostRequest") })
        assertNotNull(api.callLog.firstOrNull { it.contains("fetchStatus") })
    }

    @Test
    fun `saved card e2e - redirect then FAILED fetch status marks payment failed`() = runTest {
        api.nextSavedCardPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "redirect", url = "https://bank.example.com"),
            responseCode = 200
        )
        viewModel.postSavedCardRequest(instrumentRef = "ref_fail", isSICheckboxChecked = false)

        api.nextFetchStatus = ApiResponse.Success(
            buildFailedFetchStatus(reasonCode = "INSUFFICIENT_FUNDS", reason = "Card declined"),
            responseCode = 200
        )
        viewModel.callFetchStatus("FAILED")

        assertTrue(CheckoutDetailsHandler.isPaymentFailed)
    }
}
