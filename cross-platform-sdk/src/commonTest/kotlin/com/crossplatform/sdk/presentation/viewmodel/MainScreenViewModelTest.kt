package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AppliedOfferResponse
import com.crossplatform.sdk.data.model.PaymentMethod
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
}
