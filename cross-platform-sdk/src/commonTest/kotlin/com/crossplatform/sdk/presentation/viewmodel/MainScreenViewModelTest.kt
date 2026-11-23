package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AppliedOfferResponse
import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import com.crossplatform.sdk.fakes.FakeCallUIAnalyticsRepo
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers [MainScreenViewModel] methods that aren't already exercised
 * end-to-end through [MainScreenScreenTest] (which drives `loadSession()`
 * via the real screen). This file targets the standalone methods directly:
 * offer application/removal and base64 decoding.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

    private lateinit var mainScreenRepo: FakeMainScreenRepo
    private lateinit var otherPaymentMethodRepo: FakeOtherPaymentMethodRepo
    private lateinit var instantOfferRepo: FakeInstantOfferRepo
    private lateinit var analyticsRepo: FakeCallUIAnalyticsRepo
    private lateinit var fetchStatusRepo: FakeFetchStatusRepo
    private lateinit var viewModel: MainScreenViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mainScreenRepo = FakeMainScreenRepo()
        otherPaymentMethodRepo = FakeOtherPaymentMethodRepo()
        instantOfferRepo = FakeInstantOfferRepo()
        analyticsRepo = FakeCallUIAnalyticsRepo()
        fetchStatusRepo = FakeFetchStatusRepo()
        // loadSession() would fire in init{} and fail fast (empty token) —
        // harmless here since these tests call other methods directly.
        viewModel = MainScreenViewModel(mainScreenRepo, analyticsRepo, otherPaymentMethodRepo, instantOfferRepo, fetchStatusRepo)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── decodeBase64Url ──────────────────────────────────────────────────────

    @Test
    fun `decodeBase64Url decodes a standard base64 string`() {
        // "hello world" base64-encoded
        val result = viewModel.decodeBase64Url("aGVsbG8gd29ybGQ=")

        assertEquals("hello world", result)
    }

    @Test
    fun `decodeBase64Url on invalid input marks the session failed and returns the error text`() {
        val before = CheckoutDetailsHandler.checkoutDetails.isPaymentFailed

        val result = viewModel.decodeBase64Url("not-valid-base64!!!")

        assertTrue(result.isNotBlank())
        assertEquals(!before, CheckoutDetailsHandler.checkoutDetails.isPaymentFailed) // setSessionFailed() toggles it
    }

    // ── applyOffer / removeOffer ────────────────────────────────────────────

    @Test
    fun `applyOffer updates the amount by the applied discount and refetches payment methods`() = runTest {
        instantOfferRepo.applyOfferResult = ApiResponse.Success(
            AppliedOfferResponse(
                originalAmount = 1000.0,
                evaluatedOffers = listOf(AppliedOfferResponse.EvaluatedOffers(title = "Save10", description = null, appliedDiscountAmount = 100.0)),
                finalAmount = 900.0,
            ),
            responseCode = 200,
        )
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(listOf(PaymentMethod(id = "pm_1", type = "Card", brand = "Card")), responseCode = 200)
        mainScreenRepo.surchargeResult = ApiResponse.Error(message = "no surcharge")

        viewModel.applyOffer(selectedCode = "SAVE10", amount = 1000.0)

        assertEquals(900.0, CheckoutDetailsHandler.checkoutDetails.amount)
        assertEquals("SAVE10", CheckoutDetailsHandler.checkoutDetails.appliedOfferId)
        assertEquals(false, viewModel.isBoxPayAnimationLoading.value)
        assertEquals(900.0, otherPaymentMethodRepo.lastGetPaymentMethodsAmount)
    }

    @Test
    fun `applyOffer with no evaluated offers treats the discount as zero`() = runTest {
        instantOfferRepo.applyOfferResult = ApiResponse.Success(
            AppliedOfferResponse(originalAmount = 1000.0, evaluatedOffers = emptyList(), finalAmount = 1000.0),
            responseCode = 200,
        )
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        mainScreenRepo.surchargeResult = ApiResponse.Error(message = "no surcharge")

        viewModel.applyOffer(selectedCode = "SAVE10", amount = 1000.0)

        assertEquals(1000.0, CheckoutDetailsHandler.checkoutDetails.amount) // unchanged, discount was 0
    }

    @Test
    fun `applyOffer turns off the loading animation on error without changing the amount`() = runTest {
        val amountBefore = CheckoutDetailsHandler.checkoutDetails.amount
        instantOfferRepo.applyOfferResult = ApiResponse.Error(message = "offer invalid")

        viewModel.applyOffer(selectedCode = "BAD_CODE", amount = 1000.0)

        assertEquals(false, viewModel.isBoxPayAnimationLoading.value)
        assertEquals(amountBefore, CheckoutDetailsHandler.checkoutDetails.amount)
    }

    @Test
    fun `removeOffer restores the original amount by adding back the discount and clears the applied offer`() = runTest {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        mainScreenRepo.surchargeResult = ApiResponse.Error(message = "no surcharge")

        viewModel.removeOffer(discountAmount = 100.0, amount = 900.0)

        assertEquals(1000.0, CheckoutDetailsHandler.checkoutDetails.amount) // 900 + 100
        assertEquals("", CheckoutDetailsHandler.checkoutDetails.appliedOfferId)
        assertEquals(false, viewModel.isBoxPayAnimationLoading.value)
    }

    // ── callCheckoutSessionExpireModal / callCheckoutSessionSuccessModal ─────

    @Test
    fun `callCheckoutSessionExpireModal records EXPIRED status and flips the expired flag once`() {
        val before = CheckoutDetailsHandler.checkoutDetails.isSessionExpired

        viewModel.callCheckoutSessionExpireModal(transactionId = "txn_exp")

        val details = CheckoutDetailsHandler.checkoutDetails
        assertEquals("EXPIRED", details.status)
        assertEquals("txn_exp", details.transactionId)
        assertEquals(!before, details.isSessionExpired)
    }

    @Test
    fun `callCheckoutSessionSuccessModal records SUCCESS status, method and time and flips the success flag once`() {
        val before = CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful

        viewModel.callCheckoutSessionSuccessModal(
            transactionId = "txn_ok",
            dateNTime = "18/11/2026 17:10",
            paymentMethod = "VISA"
        )

        val details = CheckoutDetailsHandler.checkoutDetails
        assertEquals("SUCCESS", details.status)
        assertEquals("txn_ok", details.transactionId)
        assertEquals(!before, details.isPaymentSuccessful)
    }

    @Test
    fun `callCheckoutSessionSuccessModal records PROCESSING status and still shows the success page`() {
        val before = CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful

        viewModel.callCheckoutSessionSuccessModal(
            transactionId = "txn_proc",
            dateNTime = "18/11/2026 17:10",
            paymentMethod = "VISA",
            status = TransactionStatusEnum.PROCESSING
        )

        val details = CheckoutDetailsHandler.checkoutDetails
        assertEquals("PROCESSING", details.status)
        assertEquals("txn_proc", details.transactionId)
        assertEquals(!before, details.isPaymentSuccessful)
    }
    // ── postEditSavedCard ────────────────────────────────────────────────────

    private fun savedCard(id: String, name: String) = com.crossplatform.sdk.domain.model.SelectedPaymentMethod(
        type = "Card", id = id, displayName = name, displayValue = "•••• 1111",
        imageUrl = "", instrumentType = id, isLastUsed = null, isSelected = null,
    )

    private fun editedInstrument(nickName: String?) = com.crossplatform.sdk.data.model.RecommendedInstrumentsResponse(
        type = "Card", brand = "VISA", instrumentRef = "card_1",
        displayValue = "•••• 1111", logoUrl = null, cardNickName = nickName,
    )

    private fun seedCards() {
        val cards = listOf(savedCard("card_1", "Old name"), savedCard("card_2", "Other card"))
        viewModel.cardsRecommendedList.value = cards
        viewModel.recommendedList.value = cards
    }

    @Test
    fun `postEditSavedCard success renames the card in both lists`() = runTest {
        seedCards()
        mainScreenRepo.editSavedCardResult = ApiResponse.Success(editedInstrument("New name"), responseCode = 200)

        viewModel.postEditSavedCard("card_1", "New name")

        assertEquals(listOf("New name", "Other card"), viewModel.cardsRecommendedList.value.map { it.displayName })
        assertEquals(listOf("New name", "Other card"), viewModel.recommendedList.value.map { it.displayName })
    }

    @Test
    fun `postEditSavedCard sends the new nickname and instrument ref to the repo`() = runTest {
        seedCards()
        mainScreenRepo.editSavedCardResult = ApiResponse.Success(editedInstrument("New name"), responseCode = 200)

        viewModel.postEditSavedCard("card_1", "New name")

        assertEquals("New name", mainScreenRepo.lastEditedNickName)
        assertEquals("card_1", mainScreenRepo.lastEditedInstrumentRef)
    }

    @Test
    fun `postEditSavedCard uses the nickname returned by the server`() = runTest {
        seedCards()
        mainScreenRepo.editSavedCardResult = ApiResponse.Success(editedInstrument("Normalised"), responseCode = 200)

        viewModel.postEditSavedCard("card_1", "  Normalised  ")

        assertEquals("Normalised", viewModel.cardsRecommendedList.value.first().displayName)
    }

    @Test
    fun `postEditSavedCard falls back to the typed nickname when the server returns none`() = runTest {
        seedCards()
        mainScreenRepo.editSavedCardResult = ApiResponse.Success(editedInstrument(null), responseCode = 200)

        viewModel.postEditSavedCard("card_1", "Typed name")

        assertEquals("Typed name", viewModel.cardsRecommendedList.value.first().displayName)
    }

    @Test
    fun `postEditSavedCard reports success to the caller and stops the loader`() = runTest {
        seedCards()
        mainScreenRepo.editSavedCardResult = ApiResponse.Success(editedInstrument("New name"), responseCode = 200)
        var result: Boolean? = null

        viewModel.postEditSavedCard("card_1", "New name") { result = it }

        assertEquals(true, result)
        assertEquals(false, viewModel.isBoxPayAnimationLoading.value)
    }

    @Test
    fun `postEditSavedCard error leaves both lists unchanged and stops the loader`() = runTest {
        seedCards()
        mainScreenRepo.editSavedCardResult = ApiResponse.Error(message = "rejected")
        var result: Boolean? = null

        viewModel.postEditSavedCard("card_1", "New name") { result = it }

        assertEquals(false, result)
        assertEquals(listOf("Old name", "Other card"), viewModel.cardsRecommendedList.value.map { it.displayName })
        assertEquals(listOf("Old name", "Other card"), viewModel.recommendedList.value.map { it.displayName })
        assertEquals(false, viewModel.isBoxPayAnimationLoading.value)
    }

    @Test
    fun `deleting a saved card removes it from the recommended list too`() = runTest {
        seedCards()
        mainScreenRepo.deleteSavedCardResult = ApiResponse.Success(emptyList(), responseCode = 200)

        viewModel.onClickDeleteSavedCard("card_1")

        assertEquals(listOf("card_2"), viewModel.cardsRecommendedList.value.map { it.id })
        assertEquals(listOf("card_2"), viewModel.recommendedList.value.map { it.id })
    }

    @Test
    fun `a failed delete keeps the card in the recommended list`() = runTest {
        seedCards()
        mainScreenRepo.deleteSavedCardResult = ApiResponse.Error(message = "failed")

        viewModel.onClickDeleteSavedCard("card_1")

        assertEquals(listOf("card_1", "card_2"), viewModel.recommendedList.value.map { it.id })
}
