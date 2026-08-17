package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.Method
import com.crossplatform.sdk.data.model.PaymentActions
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.TransactionStatus
import com.crossplatform.sdk.fakes.FakeCallUIAnalyticsRepo
import com.crossplatform.sdk.fakes.FakeCardScreenRepo
import com.crossplatform.sdk.fakes.FakeFetchStatusRepo
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
import kotlin.test.assertTrue

/**
 * Covers three things for [CardScreenViewModel]:
 *  1. Pure validation logic (Luhn check, card-number/expiry field handling).
 *  2. `checkCardValid` combining all the individual field flags correctly.
 *  3. An integration-style flow test: `postCardRequest()` -> repo -> the real
 *     `handlePaymentResponse` shared function -> observable ViewModel state
 *     (`showWebview`, `htmlString`, `isBoxPayAnimationVisible`), i.e. the same
 *     state the Compose screens collect and render from.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CardScreenViewModelTest {

    private lateinit var cardRepo: FakeCardScreenRepo
    private lateinit var fetchStatusRepo: FakeFetchStatusRepo
    private lateinit var analyticsRepo: FakeCallUIAnalyticsRepo
    private lateinit var viewModel: CardScreenViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        cardRepo = FakeCardScreenRepo()
        fetchStatusRepo = FakeFetchStatusRepo()
        analyticsRepo = FakeCallUIAnalyticsRepo()
        viewModel = CardScreenViewModel(cardRepo, fetchStatusRepo, analyticsRepo)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Luhn validation ─────────────────────────────────────────────────────

    @Test
    fun `isValidCardNumberByLuhn accepts a known-valid test card number`() {
        // Standard Luhn-valid Visa test number.
        assertTrue(viewModel.isValidCardNumberByLuhn("4532015112830366"))
    }

    @Test
    fun `isValidCardNumberByLuhn rejects a number with a corrupted last digit`() {
        assertFalse(viewModel.isValidCardNumberByLuhn("4532015112830367"))
    }

    @Test
    fun `isValidCardNumberByLuhn rejects numbers shorter than 13 digits`() {
        assertFalse(viewModel.isValidCardNumberByLuhn("123456789012"))
    }

    // ── formatExpiryForApi ──────────────────────────────────────────────────

    @Test
    fun `formatExpiryForApi converts MMYY into YYYY-MM for the api`() {
        assertEquals("2030-05", viewModel.formatExpiryForApi("0530"))
    }

    @Test
    fun `formatExpiryForApi returns empty string for incomplete input`() {
        assertEquals("", viewModel.formatExpiryForApi("053"))
    }

    // ── handleCardNumberChange ──────────────────────────────────────────────

    @Test
    fun `handleCardNumberChange strips non-digits and caps at 16 digits`() {
        viewModel.handleCardNumberChange("4111 1111-1111 11119999", isTestEnv = false)

        assertEquals("4111111111111111", viewModel.cardNumberText.value)
    }

    @Test
    fun `handleCardNumberChange flags an error for a number starting with zero`() {
        viewModel.handleCardNumberChange("0111111111111111", isTestEnv = false)

        assertTrue(viewModel.cardNumberError.value)
    }

    @Test
    fun `handleCardNumberChange resets to the default card icon state when cleared`() {
        viewModel.handleCardNumberChange("4111111111", isTestEnv = false)
        viewModel.handleCardNumberChange("", isTestEnv = false)

        assertEquals("", viewModel.cardNumberText.value)
        assertEquals(3, viewModel.maxCvvLength.value)
        assertEquals(19, viewModel.maxCardNumberLength.value)
    }

    @Test
    fun `handleCardNumberChange triggers a BIN lookup once 9 digits are entered`() = runTest {
        cardRepo.getCardDetailsResult = ApiResponse.Error(message = "not found")

        viewModel.handleCardNumberChange("411111111", isTestEnv = false)

        assertEquals("411111111", cardRepo.lastGetCardDetailsCardNumber)
    }

    // ── updateCardIcon / checkCardValid ─────────────────────────────────────

    @Test
    fun `updateCardIcon widens cvv and card number length for AmericanExpress`() {
        viewModel.updateCardIcon(isTestEnv = false, brand = "AmericanExpress")

        assertEquals(4, viewModel.maxCvvLength.value)
        assertEquals(18, viewModel.maxCardNumberLength.value)
    }

    @Test
    fun `updateCardIcon in test env keeps the 19-digit cap for AmericanExpress`() {
        viewModel.updateCardIcon(isTestEnv = true, brand = "AmericanExpress")

        assertEquals(19, viewModel.maxCardNumberLength.value)
    }

    @Test
    fun `checkCardValid is true only once number, expiry, cvv and name are all valid`() {
        viewModel.cardNumberText.value = "4111111111111111" // 16 digits
        viewModel.cardExpiryText.value = "1230"
        viewModel.cardCvvText.value = "123"
        viewModel.cardHolderNameText.value = "Jane Doe"

        viewModel.checkCardValid(isTestEnv = false)

        assertTrue(viewModel.cardValid.value)
    }

    @Test
    fun `checkCardValid is false when the holder name is blank`() {
        viewModel.cardNumberText.value = "4111111111111111"
        viewModel.cardExpiryText.value = "1230"
        viewModel.cardCvvText.value = "123"
        viewModel.cardHolderNameText.value = ""

        viewModel.checkCardValid(isTestEnv = false)

        assertFalse(viewModel.cardValid.value)
    }

    @Test
    fun `checkCardValid is false when any individual field error flag is set`() {
        viewModel.cardNumberText.value = "4111111111111111"
        viewModel.cardExpiryText.value = "1230"
        viewModel.cardCvvText.value = "123"
        viewModel.cardHolderNameText.value = "Jane Doe"
        viewModel.cardCvvError.value = true

        viewModel.checkCardValid(isTestEnv = false)

        assertFalse(viewModel.cardValid.value)
    }

    // ── postCardRequest -> handlePaymentResponse integration ───────────────

    @Test
    fun `postCardRequest with an html action opens the webview with the returned html`() = runTest {
        cardRepo.postCardDetailsResult = ApiResponse.Success(
            PaymentMethodPostResponse(
                transactionId = "txn_1",
                transactionTimestampLocale = "12 Aug 2026",
                status = TransactionStatus(status = "RequiresAction", reason = "", reasonCode = ""),
                actions = listOf(PaymentActions(type = "html", htmlPageString = "<html>3DS</html>")),
                paymentMethod = Method(type = "Card", brand = "VISA"),
            ),
            responseCode = 200,
        )

        viewModel.postCardRequest(isSICheckBoxClicked = false)

        assertEquals("<html>3DS</html>", viewModel.htmlString.value)
        assertTrue(viewModel.showWebview.value)
        // both PAYMENT_CATEGORY_SELECTED and PAYMENT_INITIATED analytics events fire before the request
        assertEquals(2, analyticsRepo.events.size)
    }

    @Test
    fun `postCardRequest with a redirect action stores the payment url instead of html`() = runTest {
        cardRepo.postCardDetailsResult = ApiResponse.Success(
            PaymentMethodPostResponse(
                transactionId = "txn_2",
                transactionTimestampLocale = "12 Aug 2026",
                status = TransactionStatus(status = "RequiresAction", reason = "", reasonCode = ""),
                actions = listOf(PaymentActions(type = "redirect", url = "https://pay.example.com/redirect")),
                paymentMethod = Method(type = "Card", brand = "VISA"),
            ),
            responseCode = 200,
        )

        viewModel.postCardRequest(isSICheckBoxClicked = false)

        assertEquals("https://pay.example.com/redirect", viewModel.url.value)
        assertTrue(viewModel.showWebview.value)
    }

    @Test
    fun `postCardRequest turns off the loading animation when the api call errors`() = runTest {
        cardRepo.postCardDetailsResult = ApiResponse.Error(message = "boom")

        viewModel.postCardRequest(isSICheckBoxClicked = false)

        assertFalse(viewModel.isBoxPayAnimationVisible.value)
    }
}
