package com.crossplatform.sdk.presentation.viewmodel

import app.cash.turbine.test
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.Method
import com.crossplatform.sdk.data.model.PaymentActions
import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.TransactionStatus
import com.crossplatform.sdk.fakes.FakeCallUIAnalyticsRepo
import com.crossplatform.sdk.fakes.FakeCardScreenRepo
import com.crossplatform.sdk.fakes.FakeFetchStatusRepo
import com.crossplatform.sdk.fakes.FakeMainScreenRepo
import com.crossplatform.sdk.fakes.FakeOtherPaymentMethodRepo
import com.crossplatform.sdk.presentation.UiState
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BoxPayElementsViewModelTest {

    private lateinit var mainScreenRepo: FakeMainScreenRepo
    private lateinit var cardRepo: FakeCardScreenRepo
    private lateinit var analyticsRepo: FakeCallUIAnalyticsRepo
    private lateinit var fetchStatusRepo: FakeFetchStatusRepo
    private lateinit var otherPaymentMethodRepo: FakeOtherPaymentMethodRepo
    private lateinit var viewModel: BoxPayElementsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mainScreenRepo = FakeMainScreenRepo()
        cardRepo = FakeCardScreenRepo()
        analyticsRepo = FakeCallUIAnalyticsRepo()
        fetchStatusRepo = FakeFetchStatusRepo()
        otherPaymentMethodRepo = FakeOtherPaymentMethodRepo()
        // init{} calls loadSession(), which short-circuits safely on the
        // default empty token (same guard as MainScreenViewModel).
        viewModel = BoxPayElementsViewModel(mainScreenRepo, cardRepo, analyticsRepo, fetchStatusRepo, otherPaymentMethodRepo)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Card validation (identical logic to CardScreenViewModel) ────────────

    @Test
    fun `isValidCardNumberByLuhn accepts a known-valid card number`() {
        assertTrue(viewModel.isValidCardNumberByLuhn("4532015112830366"))
    }

    @Test
    fun `formatExpiryForApi converts MMYY into YYYY-MM`() {
        assertEquals("2030-05", viewModel.formatExpiryForApi("0530"))
    }

    @Test
    fun `handleCardNumberChange strips non-digits and caps at 16 digits`() {
        viewModel.handleCardNumberChange("4111 1111-1111 11119999", isTestEnv = false)

        assertEquals("4111111111111111", viewModel.cardNumberText.value)
    }

    @Test
    fun `checkCardValid is true only once every field is valid`() {
        viewModel.cardNumberText.value = "4111111111111111"
        viewModel.cardExpiryText.value = "1230"
        viewModel.cardCvvText.value = "123"
        viewModel.cardHolderNameText.value = "Jane Doe"

        viewModel.checkCardValid(isTestEnv = false)

        assertTrue(viewModel.cardValid.value)
    }

    // ── postCardRequest ───────────────────────────────────────────────────

    @Test
    fun `postCardRequest with an html action opens the webview`() = runTest {
        cardRepo.postCardDetailsResult = ApiResponse.Success(
            PaymentMethodPostResponse(
                transactionId = "txn_1", transactionTimestampLocale = "12 Aug 2026",
                status = TransactionStatus(status = "RequiresAction", reason = "", reasonCode = ""),
                actions = listOf(PaymentActions(type = "html", htmlPageString = "<html>3DS</html>")),
                paymentMethod = Method(type = "Card", brand = "VISA"),
            ),
            responseCode = 200,
        )

        viewModel.postCardRequest(isSICheckBoxClicked = false)

        assertEquals("<html>3DS</html>", viewModel.htmlString.value)
        assertTrue(viewModel.showWebViewScreen.value)
    }

    @Test
    fun `postCardRequest turns off the loading animation on error`() = runTest {
        cardRepo.postCardDetailsResult = ApiResponse.Error(message = "declined")

        viewModel.postCardRequest(isSICheckBoxClicked = false)

        assertEquals(false, viewModel.isBoxPayAnimationLoading.value)
    }

    // ── loadBanksList / onSearch ──────────────────────────────────────────

    @Test
    fun `loadBanksList maps and sorts the requested payment method type`() = runTest {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(
            listOf(
                PaymentMethod(id = "sbi", type = "NetBanking", brand = "NetBanking", title = "SBI"),
                PaymentMethod(id = "hdfc", type = "NetBanking", brand = "NetBanking", title = "HDFC"),
            ),
            responseCode = 200,
        )

        viewModel.loadBanksList(type = "netbanking")

        assertIs<UiState.Success<*>>(viewModel.uiState.value)
        val names = (viewModel.uiState.value as UiState.Success<List<*>>).data
            .map { (it as com.crossplatform.sdk.domain.model.SelectedPaymentMethod).displayName }
        assertEquals(listOf("HDFC", "SBI"), names)
    }

    @Test
    fun `onSearch filters the loaded list client-side`() = runTest {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(
            listOf(
                PaymentMethod(id = "sbi", type = "NetBanking", brand = "NetBanking", title = "SBI"),
                PaymentMethod(id = "hdfc", type = "NetBanking", brand = "NetBanking", title = "HDFC"),
            ),
            responseCode = 200,
        )
        viewModel.loadBanksList(type = "netbanking")

        viewModel.onSearch("hd")

        val names = (viewModel.uiState.value as UiState.Success<List<*>>).data
            .map { (it as com.crossplatform.sdk.domain.model.SelectedPaymentMethod).displayName }
        assertEquals(listOf("HDFC"), names)
        assertEquals("hd", viewModel.netBankingSearchQuery.value)
    }

    // ── PaySelection -> isPayable (unique to the Elements embedded mode) ───

    @Test
    fun `isPayable starts false with no selection`() = runTest {
        viewModel.isPayable.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `a Card selection is payable only once cardValid becomes true`() = runTest {
        viewModel.isPayable.test {
            assertFalse(awaitItem()) // initial: None

            viewModel.setPaySelection(BoxPayElementsViewModel.PaySelection.Card)
            assertFalse(awaitItem()) // Card selected, but cardValid is still false

            viewModel.cardValid.value = true
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `a valid UpiCollect vpa is payable, an invalid one is not`() = runTest {
        viewModel.isPayable.test {
            assertFalse(awaitItem()) // initial

            viewModel.setPaySelection(BoxPayElementsViewModel.PaySelection.UpiCollect("not-a-vpa"))
            assertFalse(awaitItem())

            viewModel.setPaySelection(BoxPayElementsViewModel.PaySelection.UpiCollect("jane@okhdfcbank"))
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `an Instrument selection is payable as soon as its value is non-blank`() = runTest {
        viewModel.isPayable.test {
            assertFalse(awaitItem()) // initial

            viewModel.setPaySelection(BoxPayElementsViewModel.PaySelection.Instrument(value = "HDFC", type = "netbanking"))
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `a SavedCard selection is payable only when its instrumentRef is non-blank`() = runTest {
        viewModel.isPayable.test {
            assertFalse(awaitItem()) // initial

            viewModel.setPaySelection(BoxPayElementsViewModel.PaySelection.SavedCard(instrumentRef = ""))
            // blank instrumentRef -> no new distinct emission expected here since
            // both None and this evaluate to `false`; assert current state directly instead.
            assertEquals(false, viewModel.isPayable.value)

            viewModel.setPaySelection(BoxPayElementsViewModel.PaySelection.SavedCard(instrumentRef = "ref_1"))
            assertTrue(awaitItem())
        }
    }
}
