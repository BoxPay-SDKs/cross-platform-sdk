package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AppliedOfferResponse
import com.crossplatform.sdk.data.model.FetchSurchargeResponse
import com.crossplatform.sdk.domain.model.SurchargeModel
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
 * Covers the surcharge requirement from the complete-checkout-flow spec:
 * a surcharge that applies must be *visible* (surfaced through
 * [CheckoutDetailsHandler.surchargeDetailsFlow], the same flow `OrderDetails`
 * and `ShowUpdatedAmountBottomSheet` read from), and, separately, *no extra
 * surcharge should ever be applied* on top of what the backend actually
 * quoted.
 *
 * All of this routes through the same private function regardless of caller:
 * `MainScreenViewModel.fetchSurchargeAndApply(amount, currencyCode)`, invoked
 * from three places - `loadSession()`, `applyOffer()`, and `removeOffer()`.
 * That function is a pure "recompute from this base amount" operation: it
 * always does `CheckoutDetailsHandler.setAmount(amount + surcharge)` using
 * whatever `amount` it was given - it does not know or care whether a
 * surcharge is already folded into that number.
 *
 * Tests below use [FakeMainScreenRepo] and never touch `loadSession()`
 * directly (which reads a Compose resource file via `loadCountryData()` and
 * is flaky outside an Android/Robolectric runtime - see the doc comment on
 * `MainScreenScreenTest`); a "session already loaded with amount X" starting
 * point is simulated with `CheckoutDetailsHandler.setAmount(X)` directly,
 * which is equivalent for this function's purposes since it only reads
 * `checkoutDetails.amount` / `.discountAmount`, never `_state`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SurchargeApplicationIntegrationTest {

    private lateinit var mainScreenRepo: FakeMainScreenRepo
    private lateinit var otherPaymentMethodRepo: FakeOtherPaymentMethodRepo
    private lateinit var instantOfferRepo: FakeInstantOfferRepo
    private lateinit var viewModel: MainScreenViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        CheckoutDetailsHandler.resetToDefault()
        mainScreenRepo = FakeMainScreenRepo()
        otherPaymentMethodRepo = FakeOtherPaymentMethodRepo()
        instantOfferRepo = FakeInstantOfferRepo()
        // MainScreenViewModel's init{} calls loadSession(), which short-circuits
        // safely here since CheckoutDetailsHandler's token is still empty.
        viewModel = MainScreenViewModel(
            repo = mainScreenRepo,
            analyticsRepo = FakeCallUIAnalyticsRepo(),
            otherPaymentMethodRepo = otherPaymentMethodRepo,
            instantOfferRepo = instantOfferRepo,
            fetchStatusRepo = FakeFetchStatusRepo(),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        CheckoutDetailsHandler.resetToDefault()
    }

    // ── visibility: a quoted surcharge is summed in AND surfaced in the list ──

    @Test
    fun `a global surcharge fetched when an offer is applied is summed into the amount and visible via surchargeDetailsFlow`() = runTest {
        CheckoutDetailsHandler.setAmount(1000.0) // "session already loaded", no surcharge fetched yet
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        instantOfferRepo.applyOfferResult = ApiResponse.Success(
            AppliedOfferResponse(
                originalAmount = 1000.0,
                evaluatedOffers = listOf(AppliedOfferResponse.EvaluatedOffers(title = "SAVE10", description = null, appliedDiscountAmount = 100.0)),
                finalAmount = 900.0,
            ),
            responseCode = 200,
        )
        mainScreenRepo.surchargeResult = ApiResponse.Success(
            surchargeResponse(title = "Card Surcharge", applicableOn = null, network = "VISA", fee = 27.0),
            responseCode = 200,
        )

        assertEquals(emptyList(), CheckoutDetailsHandler.surchargeDetailsFlow.value) // nothing fetched yet

        viewModel.applyOffer(selectedCode = "SAVE10", amount = 1000.0)

        // surchargeDetailsFlow is the exact flow OrderDetails/ShowUpdatedAmountBottomSheet
        // collect to decide whether to render a surcharge row - asserting on
        // it directly (rather than the plain property) proves visibility
        // through the same channel the UI actually observes.
        val surcharges = CheckoutDetailsHandler.surchargeDetailsFlow.value
        assertEquals(1, surcharges.size)
        assertEquals("Card Surcharge", surcharges.single().title, "must be visible - same list OrderDetails renders from")
        assertEquals(927.0, CheckoutDetailsHandler.checkoutDetails.amount) // 1000 - 100 discount + 27 surcharge, applied exactly once
    }

    @Test
    fun `a failed surcharge fetch clears the surcharge list instead of leaving a stale one, and adds nothing to the amount`() = runTest {
        CheckoutDetailsHandler.setAmount(1000.0)
        CheckoutDetailsHandler.setSurchargeDetails(listOf(surchargeModel(title = "Stale Surcharge", amount = 15.0))) // leftover from a previous, successful fetch
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        instantOfferRepo.applyOfferResult = ApiResponse.Success(
            AppliedOfferResponse(
                originalAmount = 1000.0,
                evaluatedOffers = listOf(AppliedOfferResponse.EvaluatedOffers(title = "SAVE10", description = null, appliedDiscountAmount = 100.0)),
                finalAmount = 900.0,
            ),
            responseCode = 200,
        )
        mainScreenRepo.surchargeResult = ApiResponse.Error(message = "surcharge service unavailable")

        viewModel.applyOffer(selectedCode = "SAVE10", amount = 1000.0)

        assertEquals(emptyList(), CheckoutDetailsHandler.checkoutDetails.surchargeDetails, "a failed fetch must not leave a stale surcharge visible")
        assertEquals(900.0, CheckoutDetailsHandler.checkoutDetails.amount, "no surcharge amount should be added when the fetch fails")
    }

    @Test
    fun `only globally-applicable surcharges are auto-summed into the amount - a network-specific one is listed but not added on top`() = runTest {
        CheckoutDetailsHandler.setAmount(1000.0)
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        instantOfferRepo.applyOfferResult = ApiResponse.Success(
            AppliedOfferResponse(originalAmount = 1000.0, evaluatedOffers = emptyList(), finalAmount = 1000.0),
            responseCode = 200,
        )
        mainScreenRepo.surchargeResult = ApiResponse.Success(
            FetchSurchargeResponse(
                amountBeforeSurcharge = null,
                finalAmountAfterMarriage = null,
                appliedCharges = listOf(
                    // global - applies regardless of the chosen method
                    FetchSurchargeResponse.AppliedSurcharge(
                        surchargeDetails = FetchSurchargeResponse.SurchargeDetails(title = "Platform Fee", surchargeCode = "PF", applicableOn = null, network = null, classification = null),
                        calculatedSurchargeFee = 20.0,
                    ),
                    // only applies if the shopper pays by credit card
                    FetchSurchargeResponse.AppliedSurcharge(
                        surchargeDetails = FetchSurchargeResponse.SurchargeDetails(title = "Credit Card Fee", surchargeCode = "CC", applicableOn = "credit", network = "VISA", classification = "CONSUMER"),
                        calculatedSurchargeFee = 15.0,
                    ),
                ),
            ),
            responseCode = 200,
        )

        viewModel.applyOffer(selectedCode = "NONE", amount = 1000.0)

        // Only the global 20.0 fee is baked into the headline amount; the
        // credit-specific 15.0 fee is *not* double-counted here - it's surfaced
        // separately (via surchargeDetailsFlow) so the UI can show it only once
        // the shopper actually selects a credit card, per OrderDetails.kt's own
        // `applicable.isEmpty() || applicable == selectedPaymentMethod` filter.
        assertEquals(1020.0, CheckoutDetailsHandler.checkoutDetails.amount)
        assertEquals(2, CheckoutDetailsHandler.checkoutDetails.surchargeDetails.size, "both surcharges must still be visible in the list")
        assertTrue(CheckoutDetailsHandler.checkoutDetails.surchargeDetails.any { it.title == "Credit Card Fee" })
    }

    // ── no extra surcharge: apply/remove must not compound the fee ───────────

    @Test
    fun `REGRESSION - applying then removing an offer must return to exactly base plus one surcharge, not a compounded amount`() = runTest {
        // Reproduces MainScreen.kt's exact call convention for the offer
        // section (see OfferSection's onApply/onRemove callbacks):
        //   onApply:  viewModel.applyOffer(code, amount.value + discountAmount.value)
        //   onRemove: viewModel.removeOffer(discountAmount.value, amount.value)
        // Both pass the *current displayed amount* (which already has any
        // previously-fetched surcharge baked in) back into functions that
        // then fetch and re-apply the surcharge *again* on top of it.
        val baseAmount = 1000.0
        val surchargeFee = 50.0
        val discount = 100.0

        // "Session already loaded, with its one-time surcharge already applied."
        CheckoutDetailsHandler.setAmount(baseAmount + surchargeFee) // 1050
        CheckoutDetailsHandler.setSurchargeDetails(listOf(surchargeModel(title = "Platform Fee", amount = surchargeFee)))
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        mainScreenRepo.surchargeResult = ApiResponse.Success(surchargeResponse(title = "Platform Fee", applicableOn = null, network = null, fee = surchargeFee), responseCode = 200)
        instantOfferRepo.applyOfferResult = ApiResponse.Success(
            AppliedOfferResponse(
                originalAmount = baseAmount, evaluatedOffers = listOf(AppliedOfferResponse.EvaluatedOffers(title = "SAVE10", description = null, appliedDiscountAmount = discount)),
                finalAmount = baseAmount - discount,
            ),
            responseCode = 200,
        )

        // ── user applies the offer (exact MainScreen.kt call shape) ──
        val callAmountForApply = CheckoutDetailsHandler.checkoutDetails.amount + CheckoutDetailsHandler.checkoutDetails.discountAmount
        viewModel.applyOffer(selectedCode = "SAVE10", amount = callAmountForApply)

        val expectedAfterApply = baseAmount - discount + surchargeFee // 1000 - 100 + 50 = 950
        assertEquals(
            expectedAfterApply,
            CheckoutDetailsHandler.checkoutDetails.amount,
            "the surcharge already reflected in the pre-offer amount must not be counted a second time on top of the new discount",
        )

        // ── user removes the offer again (exact MainScreen.kt call shape) ──
        val discountForRemove = CheckoutDetailsHandler.checkoutDetails.discountAmount
        val amountForRemove = CheckoutDetailsHandler.checkoutDetails.amount
        viewModel.removeOffer(discountAmount = discountForRemove, amount = amountForRemove)

        val expectedAfterRemove = baseAmount + surchargeFee // back to exactly where the session started: 1050
        assertEquals(
            expectedAfterRemove,
            CheckoutDetailsHandler.checkoutDetails.amount,
            "removing the offer must land back on base+surcharge, not an amount inflated by repeated surcharge fetches",
        )
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun surchargeResponse(title: String, applicableOn: String?, network: String?, fee: Double) = FetchSurchargeResponse(
        amountBeforeSurcharge = null,
        finalAmountAfterMarriage = null,
        appliedCharges = listOf(
            FetchSurchargeResponse.AppliedSurcharge(
                surchargeDetails = FetchSurchargeResponse.SurchargeDetails(
                    title = title, surchargeCode = "SC1", applicableOn = applicableOn, network = network, classification = "CONSUMER",
                ),
                calculatedSurchargeFee = fee,
            ),
        ),
    )

    private fun surchargeModel(title: String, amount: Double) = SurchargeModel(
        applicableOn = "", title = title, surchargeCode = "SC1", network = "", classification = "", amount = amount,
    )
}