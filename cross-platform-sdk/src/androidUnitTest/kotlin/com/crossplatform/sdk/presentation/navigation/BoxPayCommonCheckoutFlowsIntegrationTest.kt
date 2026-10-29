package com.crossplatform.sdk.presentation.navigation

import com.crossplatform.sdk.fakes.initializeKamelApplicationContext

import androidx.test.core.app.ApplicationProvider

import androidx.compose.ui.platform.LocalInspectionMode

import androidx.compose.runtime.CompositionLocalProvider

import org.jetbrains.compose.resources.PreviewContextConfigurationEffect

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.CheckoutTheme
import com.crossplatform.sdk.data.model.Configs
import com.crossplatform.sdk.data.model.DeliveryAddress
import com.crossplatform.sdk.data.model.Method
import com.crossplatform.sdk.data.model.MerchantDetails
import com.crossplatform.sdk.data.model.Money
import com.crossplatform.sdk.data.model.PaymentContext
import com.crossplatform.sdk.data.model.PaymentDetails
import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.SessionDetails
import com.crossplatform.sdk.data.model.Shopper
import com.crossplatform.sdk.data.model.TransactionStatus
import com.crossplatform.sdk.fakes.FakeMainScreenRepo
import com.crossplatform.sdk.fakes.FakeOtherPaymentMethodRepo
import com.crossplatform.sdk.fakes.TestBoxPayCheckout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.runner.RunWith

/**
 * True end-to-end flow coverage driven **only** through [TestBoxPayCheckout] —
 * the test-only mirror of the real public entry point `BoxPayCommonCheckout`
 * (see `BoxPayCheckout.kt`). Every test here does exactly what a merchant
 * app does: supply a token, let the real session come back, and interact
 * with the real rendered screens — `MainScreen`, `NetBankingScreen`,
 * `WalletScreen`, `BNPLScreen`, `EMIScreen`, all wired through the real
 * `AppNavHost` — the same way [AppNavHostNavigationTest] proves the single
 * auto-nav Card flow. Only the network boundary (`FakeMainScreenRepo`,
 * `FakeOtherPaymentMethodRepo`) is faked; everything above it — ViewModels,
 * navigation, screens, `CheckoutDetailsHandler` — is the real production
 * code.
 *
 * This is deliberately NOT a unit test of any one screen/ViewModel in
 * isolation (see `MainScreenScreenTest`, `NetBankingScreenTest`, etc. for
 * those) — every assertion here is made after navigating through the real
 * `MainScreen` method list, so it also proves the wiring between screens is
 * correct, not just that each screen works alone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BoxPayCommonCheckoutFlowsIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mainScreenRepo: FakeMainScreenRepo
    private lateinit var otherPaymentMethodRepo: FakeOtherPaymentMethodRepo

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mainScreenRepo = FakeMainScreenRepo()
        otherPaymentMethodRepo = FakeOtherPaymentMethodRepo()
        // No surcharge configured -> fetchSurchargeAndApply() degrades to an
        // empty list on error rather than throwing (matches every other
        // screen test's default setup).
        mainScreenRepo.surchargeResult = ApiResponse.Error(message = "no surcharge")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── session builder ──────────────────────────────────────────────────────

    /**
     * A session with more than one payment method enabled, so MainScreen
     * does NOT auto-navigate — every flow below drives the real click on
     * the real method row, exactly like a shopper would.
     */
    private fun multiMethodSession(methods: List<PaymentMethod>) = SessionDetails(
        configs = Configs(paymentMethods = methods, enabledFields = emptyList()),
        paymentDetails = PaymentDetails(
            context = PaymentContext(countryCode = "IN", localeCode = "en-IN"),
            money = Money(currencySymbol = "₹", currencyCode = "INR", amount = 499.0),
            shopper = Shopper(
                firstName = "Jane", lastName = "Doe", email = "jane@example.com",
                uniqueReference = "shopper_1", deliveryAddress = DeliveryAddress(countryCode = "IN"),
            ),
            subscriptionDetails = null,
            order = null,
        ),
        merchantDetails = MerchantDetails(
            merchantName = "Test Merchant", merchantLogo = null,
            checkoutTheme = CheckoutTheme(
                primaryButtonColor = "#000000", buttonTextColor = "#FFFFFF", headerColor = "#000000",
                headerTextColor = "#FFFFFF", focusedTextInputBorderColor = "#CCCCCC",
                unfocusedTextInputBorderColor = "#DDDDDD", payButtonFontSize = "16", font = "Inter",
                payButtonBorderRadius = "8",
            ),
            customFields = emptyList(),
        ),
        sessionExpiryTimestamp = "", status = "NOACTION",
        lastPaidAtTimestamp = null, lastTransactionId = null, lastTransactionDetails = null,
    )

    private fun successResponse(transactionId: String = "txn_1") = ApiResponse.Success(
        PaymentMethodPostResponse(
            transactionId = transactionId,
            transactionTimestampLocale = "17 Aug 2026",
            status = TransactionStatus(status = "SUCCESS", reason = "", reasonCode = ""),
            paymentMethod = Method(type = "NetBanking", brand = "HDFC"),
        ),
        responseCode = 200,
    )

    private fun renderCheckout(token: String = "test_token") {
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            TestBoxPayCheckout(
                token = token,
                mainScreenRepo = mainScreenRepo,
                otherPaymentMethodRepo = otherPaymentMethodRepo,
            )
        }
    }

    // ── Card flow ─────────────────────────────────────────────────────────────

    @Test
    fun `clicking Cards from the real method list navigates to the real Card screen`() {
        mainScreenRepo.sessionDetailsResult = ApiResponse.Success(
            multiMethodSession(
                listOf(
                    PaymentMethod(id = "card_1", type = "Card", brand = "Card"),
                    PaymentMethod(id = "nb_1", type = "NetBanking", brand = "NetBanking"),
                )
            ),
            responseCode = 200,
        )

        renderCheckout()

        composeTestRule.onNodeWithText("Cards").performClick()

        composeTestRule.onNodeWithText("Pay via Card").assertExists()
    }

    // ── NetBanking flow, all the way through to a real payment outcome ───────

    @Test
    fun `clicking Bank Transfers navigates to the real NetBanking screen and lists the real banks`() {
        mainScreenRepo.sessionDetailsResult = ApiResponse.Success(
            multiMethodSession(
                listOf(
                    PaymentMethod(id = "card_1", type = "Card", brand = "Card"),
                    PaymentMethod(id = "nb_1", type = "NetBanking", brand = "NetBanking"),
                )
            ),
            responseCode = 200,
        )
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(
            listOf(
                PaymentMethod(id = "hdfc", type = "NetBanking", brand = "NetBanking", title = "HDFC Bank"),
                PaymentMethod(id = "sbi", type = "NetBanking", brand = "NetBanking", title = "SBI"),
            ),
            responseCode = 200,
        )

        renderCheckout()

        composeTestRule.onNodeWithText("Bank Transfers").performClick()

        composeTestRule.onNodeWithText("Select Bank").assertExists()
        composeTestRule.onNodeWithText("All Banks").assertExists()
        composeTestRule.onNodeWithText("HDFC Bank").assertExists()
        composeTestRule.onNodeWithText("SBI").assertExists()
    }

    @Test
    fun `completing a NetBanking payment end-to-end shows the real Payment Successful screen`() {
        mainScreenRepo.sessionDetailsResult = ApiResponse.Success(
            multiMethodSession(
                listOf(
                    PaymentMethod(id = "card_1", type = "Card", brand = "Card"),
                    PaymentMethod(id = "nb_1", type = "NetBanking", brand = "NetBanking"),
                )
            ),
            responseCode = 200,
        )
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(
            listOf(PaymentMethod(id = "hdfc", type = "NetBanking", brand = "NetBanking", title = "HDFC Bank")),
            responseCode = 200,
        )
        otherPaymentMethodRepo.initiatePaymentResult = successResponse()

        renderCheckout()

        composeTestRule.onNodeWithText("Bank Transfers").performClick()
        composeTestRule.onNodeWithText("HDFC Bank").performClick()

        // Proof the whole chain actually ran: token -> session -> method
        // list click -> bank click -> real initiatePayment call -> real
        // handlePaymentResponse -> CheckoutDetailsHandler.setSessionSuccess()
        // -> AppNavHost rendering the real PaymentSuccessful sheet.
        composeTestRule.onNodeWithText("Payment Successful").assertExists()
    }

    @Test
    fun `a failed NetBanking payment end-to-end shows the real Payment Failed screen`() {
        mainScreenRepo.sessionDetailsResult = ApiResponse.Success(
            multiMethodSession(
                listOf(
                    PaymentMethod(id = "card_1", type = "Card", brand = "Card"),
                    PaymentMethod(id = "nb_1", type = "NetBanking", brand = "NetBanking"),
                )
            ),
            responseCode = 200,
        )
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(
            listOf(PaymentMethod(id = "hdfc", type = "NetBanking", brand = "NetBanking", title = "HDFC Bank")),
            responseCode = 200,
        )
        otherPaymentMethodRepo.initiatePaymentResult = ApiResponse.Success(
            PaymentMethodPostResponse(
                transactionId = "txn_failed",
                transactionTimestampLocale = "17 Aug 2026",
                status = TransactionStatus(status = "FAILED", reason = "Declined by issuer", reasonCode = "DECLINED"),
                paymentMethod = Method(type = "NetBanking", brand = "HDFC"),
            ),
            responseCode = 200,
        )

        renderCheckout()

        composeTestRule.onNodeWithText("Bank Transfers").performClick()
        composeTestRule.onNodeWithText("HDFC Bank").performClick()

        composeTestRule.onNodeWithText("Payment Failed").assertExists()
    }

    // ── Wallet flow ───────────────────────────────────────────────────────────

    @Test
    fun `clicking Wallet navigates to the real Wallet screen and lists the real wallets`() {
        mainScreenRepo.sessionDetailsResult = ApiResponse.Success(
            multiMethodSession(
                listOf(
                    PaymentMethod(id = "card_1", type = "Card", brand = "Card"),
                    // brand must not be GooglePay/ApplePay/RevolutPay, or MainScreenMapper
                    // routes it to a dedicated express-checkout flag instead of the
                    // generic "Wallet" row this test is driving through.
                    PaymentMethod(id = "wallet_1", type = "Wallet", brand = "PayTM"),
                )
            ),
            responseCode = 200,
        )
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(
            listOf(PaymentMethod(id = "gpay", type = "Wallet", brand = "Wallet", title = "Google Pay")),
            responseCode = 200,
        )

        renderCheckout()

        composeTestRule.onNodeWithText("Wallet").performClick()

        composeTestRule.onNodeWithText("Select Wallet").assertExists()
        composeTestRule.onNodeWithText("All Wallet").assertExists()
        composeTestRule.onNodeWithText("Google Pay").assertExists()
    }

    // ── BNPL flow ─────────────────────────────────────────────────────────────

    @Test
    fun `clicking Buy Now Pay Later navigates to the real BNPL screen and lists the real providers`() {
        mainScreenRepo.sessionDetailsResult = ApiResponse.Success(
            multiMethodSession(
                listOf(
                    PaymentMethod(id = "card_1", type = "Card", brand = "Card"),
                    // brand must not be "Affirm", or MainScreenMapper routes it to a
                    // dedicated flag instead of the generic "Buy Now Pay Later" row.
                    PaymentMethod(id = "bnpl_1", type = "BuyNowPayLater", brand = "Simpl"),
                )
            ),
            responseCode = 200,
        )
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(
            listOf(PaymentMethod(id = "simpl", type = "BuyNowPayLater", brand = "BuyNowPayLater", title = "Simpl")),
            responseCode = 200,
        )

        renderCheckout()

        composeTestRule.onNodeWithText("Buy Now Pay Later").performClick()

        composeTestRule.onNodeWithText("Select BNPL").assertExists()
        composeTestRule.onNodeWithText("Simpl").assertExists()
    }

    // ── EMI flow ──────────────────────────────────────────────────────────────

    @Test
    fun `clicking EMI navigates to the real EMI screen and lists the real banks`() {
        mainScreenRepo.sessionDetailsResult = ApiResponse.Success(
            multiMethodSession(
                listOf(
                    PaymentMethod(id = "card_1", type = "Card", brand = "Card"),
                    PaymentMethod(id = "emi_1", type = "Emi", brand = "Emi"),
                )
            ),
            responseCode = 200,
        )
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(
            listOf(
                PaymentMethod(
                    id = "pm_1", type = "Emi", brand = "Emi", title = "HDFC Credit Card EMI", logoUrl = null,
                    emiMethod = com.crossplatform.sdk.data.model.EmiMethod(
                        duration = 3, effectiveInterestRate = 12.0, merchantBorneInterestRate = 0.0,
                        issuerTitle = "HDFC Bank", issuer = "HDFC",
                        processingFee = com.crossplatform.sdk.data.model.ProcessingFee(amountLocaleFull = "₹0"),
                        netAmountLocaleFull = "₹5,000", totalAmountLocaleFull = "₹5,100",
                        emiAmountLocaleFull = "₹1,700", merchantBorneInterestAmountLocaleFull = "₹0",
                        bankChargedInterestAmountLocaleFull = "₹100", interestChargedAmountLocaleFull = "₹50",
                        cardlessEmiProviderTitle = null, cardlessEmiProviderValue = null,
                    ),
                )
            ),
            responseCode = 200,
        )

        renderCheckout()

        composeTestRule.onNodeWithText("EMI").performClick()

        composeTestRule.onNodeWithText("All Banks").assertExists()
        composeTestRule.onNodeWithText("HDFC Bank").assertExists()
    }
}
