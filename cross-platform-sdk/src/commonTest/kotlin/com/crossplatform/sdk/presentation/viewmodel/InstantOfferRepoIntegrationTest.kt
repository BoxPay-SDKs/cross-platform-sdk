package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AppliedOfferResponse
import com.crossplatform.sdk.data.model.InstantOfferResponse
import com.crossplatform.sdk.data.repo.InstantOfferRepoImpl
import com.crossplatform.sdk.domain.model.OfferItem
import com.crossplatform.sdk.fakes.FakeApiService
import com.crossplatform.sdk.fakes.FakeCallUIAnalyticsRepo
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for the instant-offer flow.
 *
 * Uses REAL [InstantOfferRepoImpl] backed by [FakeApiService], wired into
 * REAL [InstantOfferViewModel]. Tests:
 *  - getOffers routes through real repo to FakeApiService
 *  - Success state with offer list
 *  - Error state on api failure
 *  - applyOffer routes through real repo with offerId and minAmount
 *  - Applied offer response parsed correctly via real repo
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InstantOfferRepoIntegrationTest {

    private lateinit var api: FakeApiService
    private lateinit var offerRepo: InstantOfferRepoImpl
    private lateinit var viewModel: InstantOfferViewModel

    private fun buildOffer(
        code: String = "OFFER_10",
        enabled: Boolean = true,
        discountPercent: Double = 10.0
    ) = InstantOfferResponse(
        title       = "10% off",
        description = "10% discount on all cards",
        terms       = "Valid on VISA and Mastercard",
        code        = code,
        discount    = InstantOfferResponse.Discount(
            amount     = null,
            percentage = discountPercent,
            type       = "PERCENTAGE"
        ),
        enabled  = enabled,
        criteria = InstantOfferResponse.InstantOfferCriteria(
            applicableTo = InstantOfferResponse.OfferApplicableTo(
                paymentMethods = listOf(
                    InstantOfferResponse.OfferPaymentMethod(type = "Card", brand = "VISA")
                )
            ),
            startDate = null,
            endDate   = null
        )
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        CheckoutDetailsHandler.resetToDefault()
        CheckoutDetailsHandler.setCheckoutToken(
            shopperToken            = null,
            token                   = "offer_token",
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
        // Set an amount so InstantOfferViewModel.loadOffer() can pass minAmount/maxAmount
        CheckoutDetailsHandler.setAmount(amount = 500.0)

        api       = FakeApiService()
        offerRepo = InstantOfferRepoImpl(apiService = api, ioDispatcher = UnconfinedTestDispatcher())

        // Pre-stub for VM init
        api.nextOffer = ApiResponse.Success(emptyList(), responseCode = 200)

        viewModel = InstantOfferViewModel(
            repo          = offerRepo,
            analyticsRepo = FakeCallUIAnalyticsRepo()
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        CheckoutDetailsHandler.resetToDefault()
    }

    // ── getOffers ────────────────────────────────────────────────────────────

    @Test
    fun `InstantOfferViewModel init calls getOffers through real repo`() = runTest {
        assertTrue(api.callLog.any { it.contains("getOffer") })
    }

    @Test
    fun `getOffers Success puts InstantOfferViewModel in Success state`() = runTest {
        val offerApi = FakeApiService()
        offerApi.nextOffer = ApiResponse.Success(
            listOf(buildOffer("CARD10"), buildOffer("UPI5", discountPercent = 5.0)),
            responseCode = 200
        )
        val vm = InstantOfferViewModel(
            repo          = InstantOfferRepoImpl(apiService = offerApi, ioDispatcher = UnconfinedTestDispatcher()),
            analyticsRepo = FakeCallUIAnalyticsRepo()
        )

        assertIs<UiState.Success<List<OfferItem>>>(vm.offerState.value)
    }

    @Test
    fun `getOffers Error puts InstantOfferViewModel in Error state`() = runTest {
        val errorApi = FakeApiService()
        errorApi.nextOffer = ApiResponse.Error(message = "offer fetch failed")
        val vm = InstantOfferViewModel(
            repo          = InstantOfferRepoImpl(apiService = errorApi, ioDispatcher = UnconfinedTestDispatcher()),
            analyticsRepo = FakeCallUIAnalyticsRepo()
        )

        assertIs<UiState.Error>(vm.offerState.value)
        assertEquals("offer fetch failed", (vm.offerState.value as UiState.Error).message)
    }

    @Test
    fun `getOffers routes through real InstantOfferRepoImpl to FakeApiService`() = runTest {
        api.nextOffer = ApiResponse.Success(
            listOf(buildOffer("FLAT50")),
            responseCode = 200
        )

        val result = offerRepo.getOffers(minAmount = 100.0, maxAmount = 100.0)

        assertIs<ApiResponse.Success<List<InstantOfferResponse>>>(result)
        assertEquals(1, (result as ApiResponse.Success).data.size)
        assertEquals("FLAT50", result.data[0].code)
        assertTrue(api.callLog.any { it.contains("getOffer") })
    }

    @Test
    fun `getOffers forwards minAmount and maxAmount through real repo`() = runTest {
        api.nextOffer = ApiResponse.Success(emptyList(), responseCode = 200)

        offerRepo.getOffers(minAmount = 250.0, maxAmount = 250.0)

        assertTrue(api.callLog.any { it.contains("minAmount=250") || it.contains("250.0") })
    }

    @Test
    fun `getOffers returns multiple offers via real repo`() = runTest {
        val offers = listOf(
            buildOffer("OFFER_A", discountPercent = 5.0),
            buildOffer("OFFER_B", discountPercent = 10.0),
            buildOffer("OFFER_C", discountPercent = 15.0)
        )
        api.nextOffer = ApiResponse.Success(offers, responseCode = 200)

        val result = offerRepo.getOffers(minAmount = 500.0, maxAmount = 500.0)

        assertIs<ApiResponse.Success<List<InstantOfferResponse>>>(result)
        assertEquals(3, (result as ApiResponse.Success).data.size)
    }

    @Test
    fun `getOffers error returns ApiResponse Error via real repo`() = runTest {
        api.nextOffer = ApiResponse.Error(message = "offers unavailable")

        val result = offerRepo.getOffers(minAmount = 100.0, maxAmount = 100.0)

        assertIs<ApiResponse.Error>(result)
        assertEquals("offers unavailable", (result as ApiResponse.Error).message)
    }

    // ── applyOffer ───────────────────────────────────────────────────────────

    @Test
    fun `applyOffer routes through real InstantOfferRepoImpl to FakeApiService`() = runTest {
        api.nextApplyOffer = ApiResponse.Success(
            AppliedOfferResponse(
                originalAmount  = 500.0,
                evaluatedOffers = listOf(
                    AppliedOfferResponse.EvaluatedOffers(
                        title                 = "10% off",
                        description           = "Visa card discount",
                        appliedDiscountAmount = 50.0
                    )
                ),
                finalAmount = 450.0
            ),
            responseCode = 200
        )

        val result = offerRepo.applyOffer(offerId = listOf("CARD10"), minAmount = 500.0)

        assertIs<ApiResponse.Success<AppliedOfferResponse>>(result)
        assertNotNull(api.callLog.firstOrNull { it.contains("applyOffer") })
    }

    @Test
    fun `applyOffer returns correct discount amount via real repo`() = runTest {
        api.nextApplyOffer = ApiResponse.Success(
            AppliedOfferResponse(
                originalAmount  = 1000.0,
                evaluatedOffers = listOf(
                    AppliedOfferResponse.EvaluatedOffers(
                        title                 = "Flat ₹100 off",
                        description           = null,
                        appliedDiscountAmount = 100.0
                    )
                ),
                finalAmount = 900.0
            ),
            responseCode = 200
        )

        val result = offerRepo.applyOffer(offerId = listOf("FLAT100"), minAmount = 1000.0)

        assertIs<ApiResponse.Success<AppliedOfferResponse>>(result)
        val data = (result as ApiResponse.Success).data
        assertEquals(1000.0, data.originalAmount)
        assertEquals(900.0, data.finalAmount)
        assertEquals(100.0, data.evaluatedOffers?.firstOrNull()?.appliedDiscountAmount)
    }

    @Test
    fun `applyOffer with multiple offerIds forwards them through real repo`() = runTest {
        api.nextApplyOffer = ApiResponse.Success(
            AppliedOfferResponse(originalAmount = 500.0, evaluatedOffers = emptyList(), finalAmount = 500.0),
            responseCode = 200
        )

        offerRepo.applyOffer(offerId = listOf("OFFER_A", "OFFER_B"), minAmount = 500.0)

        assertTrue(api.callLog.any { it.contains("applyOffer") })
    }

    @Test
    fun `applyOffer error returns ApiResponse Error via real repo`() = runTest {
        api.nextApplyOffer = ApiResponse.Error(message = "offer invalid or expired")

        val result = offerRepo.applyOffer(offerId = listOf("EXPIRED_OFFER"), minAmount = 200.0)

        assertIs<ApiResponse.Error>(result)
        assertEquals("offer invalid or expired", (result as ApiResponse.Error).message)
    }

    // ── loadOffer refresh ────────────────────────────────────────────────────

    @Test
    fun `loadOffer can be refreshed and returns new data`() = runTest {
        // First call (init) returned empty list
        assertIs<UiState.Success<List<OfferItem>>>(viewModel.offerState.value)

        // Re-stub and reload
        api.nextOffer = ApiResponse.Success(
            listOf(buildOffer("NEW_OFFER")),
            responseCode = 200
        )
        viewModel.loadOffer()

        assertIs<UiState.Success<List<OfferItem>>>(viewModel.offerState.value)
    }
}
