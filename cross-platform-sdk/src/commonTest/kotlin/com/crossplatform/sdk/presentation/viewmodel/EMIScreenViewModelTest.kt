package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.EmiMethod
import com.crossplatform.sdk.data.model.Method
import com.crossplatform.sdk.data.model.PaymentActions
import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.ProcessingFee
import com.crossplatform.sdk.data.model.TransactionStatus
import com.crossplatform.sdk.domain.model.Bank
import com.crossplatform.sdk.fakes.FakeCallUIAnalyticsRepo
import com.crossplatform.sdk.fakes.FakeCardScreenRepo
import com.crossplatform.sdk.fakes.FakeFetchStatusRepo
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EMIScreenViewModelTest {

    private lateinit var otherPaymentMethodRepo: FakeOtherPaymentMethodRepo
    private lateinit var cardScreenRepo: FakeCardScreenRepo
    private lateinit var analyticsRepo: FakeCallUIAnalyticsRepo
    private lateinit var fetchStatusRepo: FakeFetchStatusRepo

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        otherPaymentMethodRepo = FakeOtherPaymentMethodRepo()
        cardScreenRepo = FakeCardScreenRepo()
        analyticsRepo = FakeCallUIAnalyticsRepo()
        fetchStatusRepo = FakeFetchStatusRepo()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(): EMIScreenViewModel =
        EMIScreenViewModel(otherPaymentMethodRepo, cardScreenRepo, analyticsRepo, fetchStatusRepo)

    private fun emiPaymentMethod() = PaymentMethod(
        id = "pm_1", type = "Emi", brand = "Emi", title = "HDFC Credit Card EMI", logoUrl = null,
        emiMethod = EmiMethod(
            duration = 3, effectiveInterestRate = 12.0, merchantBorneInterestRate = 0.0,
            issuerTitle = "HDFC Bank", issuer = "HDFC", processingFee = ProcessingFee(amountLocaleFull = "\u20b90"),
            netAmountLocaleFull = "\u20b95,000", totalAmountLocaleFull = "\u20b95,100", emiAmountLocaleFull = "\u20b91,700",
            merchantBorneInterestAmountLocaleFull = "\u20b90", bankChargedInterestAmountLocaleFull = "\u20b9100",
            interestChargedAmountLocaleFull = "\u20b950", cardlessEmiProviderTitle = null, cardlessEmiProviderValue = null,
        ),
    )

    // ── loadPaymentMethods (init) ────────────────────────────────────────────

    @Test
    fun `on init, a successful response is mapped into uiState and emiBankList`() = runTest {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(listOf(emiPaymentMethod()), responseCode = 200)

        val viewModel = newViewModel()

        assertIs<UiState.Success<*>>(viewModel.uiState.value)
        assertEquals(listOf("Credit Card"), viewModel.emiBankList.value.cards.map { it.cardType })
    }

    @Test
    fun `on init, an error response is surfaced as UiState_Error`() = runTest {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Error(message = "no EMI options")

        val viewModel = newViewModel()

        assertIs<UiState.Error>(viewModel.uiState.value)
        assertEquals("no EMI options", (viewModel.uiState.value as UiState.Error).message)
    }

    // ── step navigation ──────────────────────────────────────────────────────

    @Test
    fun `starts on the Content step`() = runTest {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)

        assertEquals(EmiStep.Content, newViewModel().currentStep)
    }

    @Test
    fun `onClickBank advances to Tenure and toggles the same bank off on a second click`() = runTest {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        val viewModel = newViewModel()
        val bank = Bank(
            iconUrl = "", name = "HDFC Bank", percent = 0.0, noCostApplied = false, lowCostApplied = false,
            emiList = emptyList(), cardLessEmiValue = "", issuerBrand = "HDFC",
        )

        viewModel.onClickBank(bank)
        assertEquals(EmiStep.Tenure, viewModel.currentStep)
        assertEquals("HDFC Bank", viewModel.selectedBank.value?.name)

        viewModel.onClickBank(bank) // clicking the same bank again deselects it
        assertEquals(null, viewModel.selectedBank.value)
    }

    @Test
    fun `onProceedEmi stores the chosen tenure's terms and advances to Card`() = runTest {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        val viewModel = newViewModel()

        viewModel.onProceedEmi(percent = 0.0, isLowCost = false, isNoCost = true, dis = "\u20b9100", net = "\u20b95000")

        assertEquals(EmiStep.Card, viewModel.currentStep)
        assertTrue(viewModel.isNoCostSelected.value)
        assertEquals("\u20b9100", viewModel.discount.value)
        assertEquals("\u20b95000", viewModel.netAmount.value)
    }

    @Test
    fun `goBackStep pops the last step and returns false once only Content remains`() = runTest {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        val viewModel = newViewModel()

        viewModel.goToTenure()
        assertTrue(viewModel.goBackStep())
        assertEquals(EmiStep.Content, viewModel.currentStep)
        assertEquals(false, viewModel.goBackStep()) // nothing left to pop
    }

    // ── search / filter ──────────────────────────────────────────────────────

    @Test
    fun `onClickCard resets search text and filter`() = runTest {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        val viewModel = newViewModel()
        viewModel.onEditSearchText("hdfc")
        viewModel.onToggleFilter("NoCost")

        viewModel.onClickCard("Debit Card")

        assertEquals("Debit Card", viewModel.selectedCard.value)
        assertEquals("", viewModel.searchText.value)
        assertEquals("", viewModel.selectedFilter.value)
    }

    @Test
    fun `onToggleFilter selecting the same filter twice clears it`() = runTest {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        val viewModel = newViewModel()

        viewModel.onToggleFilter("NoCost")
        assertEquals("NoCost", viewModel.selectedFilter.value)

        viewModel.onToggleFilter("NoCost")
        assertEquals("", viewModel.selectedFilter.value)
    }

    // ── postEMIRequest ────────────────────────────────────────────────────────

    @Test
    fun `postEMIRequest with an html action opens the webview and fires both analytics events`() = runTest {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        otherPaymentMethodRepo.initiateEMIPaymentResult = ApiResponse.Success(
            PaymentMethodPostResponse(
                transactionId = "txn_1", transactionTimestampLocale = "12 Aug 2026",
                status = TransactionStatus(status = "RequiresAction", reason = "", reasonCode = ""),
                actions = listOf(PaymentActions(type = "html", htmlPageString = "<html>3DS</html>")),
                paymentMethod = Method(type = "Emi", brand = "Emi"),
            ),
            responseCode = 200,
        )
        val viewModel = newViewModel()
        viewModel.selectedEmi.value = Pair(6, "\u20b91,700")

        viewModel.postEMIRequest()

        assertEquals("<html>3DS</html>", viewModel.htmlString.value)
        assertTrue(viewModel.showWebview.value)
        assertEquals(2, analyticsRepo.events.size)
        assertEquals(6, otherPaymentMethodRepo.lastInitiateEMIPaymentDuration)
    }

    @Test
    fun `postEMIRequest turns off the loading animation on error`() = runTest {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        otherPaymentMethodRepo.initiateEMIPaymentResult = ApiResponse.Error(message = "declined")
        val viewModel = newViewModel()

        viewModel.postEMIRequest()

        assertEquals(false, viewModel.isBoxPayAnimationVisible.value)
    }

    // ── shared card-field validation (same logic as CardScreenViewModel) ────

    @Test
    fun `isValidCardNumberByLuhn accepts a known-valid card number`() = runTest {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)

        assertTrue(newViewModel().isValidCardNumberByLuhn("4532015112830366"))
    }

    @Test
    fun `formatExpiryForApi converts MMYY to YYYY-MM`() = runTest {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)

        assertEquals("2030-05", newViewModel().formatExpiryForApi("0530"))
    }
}
