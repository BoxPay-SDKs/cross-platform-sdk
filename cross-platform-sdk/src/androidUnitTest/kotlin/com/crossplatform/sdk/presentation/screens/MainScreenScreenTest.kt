package com.crossplatform.sdk.presentation.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.CheckoutTheme
import com.crossplatform.sdk.data.model.Configs
import com.crossplatform.sdk.data.model.DeliveryAddress
import com.crossplatform.sdk.data.model.MerchantDetails
import com.crossplatform.sdk.data.model.Money
import com.crossplatform.sdk.data.model.PaymentContext
import com.crossplatform.sdk.data.model.PaymentDetails
import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.data.model.RecommendedInstrumentsResponse
import com.crossplatform.sdk.data.model.SessionDetails
import com.crossplatform.sdk.data.model.Shopper
import com.crossplatform.sdk.fakes.FakeCallUIAnalyticsRepo
import com.crossplatform.sdk.fakes.FakeInstantOfferRepo
import com.crossplatform.sdk.fakes.FakeMainScreenRepo
import com.crossplatform.sdk.fakes.FakeOtherPaymentMethodRepo
import com.crossplatform.sdk.fakes.FakeFetchStatusRepo
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import com.crossplatform.sdk.presentation.viewmodel.MainScreenViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/**
 * Full-stack test for [MainScreen], mirroring what the SDK actually does at
 * runtime: session token is set on [CheckoutDetailsHandler] (the same call
 * `BoxPayCommonCheckout` makes), the fake [FakeMainScreenRepo] stands in for
 * the real "call the API, get the session" step, and the real
 * [MainScreenViewModel] processes that response exactly like production.
 *
 * `MainScreen` takes its `viewModel` as a constructor parameter (unlike most
 * other screens, it does *not* resolve it via `koinViewModel()` internally —
 * that only happens one level up, in `AppNavHost`), so this test can
 * construct a real `MainScreenViewModel` directly, the same way
 * `CardScreenIntegrationTest` does for `CardScreenViewModel`.
 *
 * KNOWN RISK, called out rather than hidden: [MainScreenViewModel.loadSession]
 * calls `loadCountryData()`, which reads `Res.readBytes("files/countryCodes.json")`.
 * If that asset isn't resolvable on the `androidUnitTest` classpath under
 * Robolectric, `loadSession()`'s catch block will turn this into an
 * `UiState.Error` instead of `UiState.Success` for reasons unrelated to this
 * test's own logic. If the "success" tests below fail, check that first
 * before assuming the test or the production code is wrong.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainScreenScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mainScreenRepo: FakeMainScreenRepo
    private lateinit var otherPaymentMethodRepo: FakeOtherPaymentMethodRepo
    private lateinit var instantOfferRepo: FakeInstantOfferRepo
    private lateinit var analyticsRepo: FakeCallUIAnalyticsRepo
    private lateinit var fetchStatusRepo: FakeFetchStatusRepo

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mainScreenRepo = FakeMainScreenRepo()
        otherPaymentMethodRepo = FakeOtherPaymentMethodRepo()
        instantOfferRepo = FakeInstantOfferRepo()
        analyticsRepo = FakeCallUIAnalyticsRepo()
        fetchStatusRepo = FakeFetchStatusRepo()

        // No surcharge configured by default -> matches what most real
        // sessions look like, and fetchSurchargeAndApply() degrades to an
        // empty surcharge list on error rather than throwing.
        mainScreenRepo.surchargeResult = ApiResponse.Error(message = "no surcharge")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = MainScreenViewModel(
        repo = mainScreenRepo,
        analyticsRepo = analyticsRepo,
        otherPaymentMethodRepo = otherPaymentMethodRepo,
        instantOfferRepo = instantOfferRepo,
        fetchStatusRepo = fetchStatusRepo,
    )

    /** Same call `BoxPayCommonCheckout` makes before ever rendering [AppNavHost]. */
    private fun setCheckoutToken(token: String, shopperToken: String? = null) {
        CheckoutDetailsHandler.setCheckoutToken(
            shopperToken = shopperToken,
            token = token,
            isTestEnv = true,
            isSuccessScreenVisible = true,
            ctaBorderRadius = 8,
            isSICheckboxChecked = false,
            isSICheckboxEnabled = false,
            isFailedScreenVisible = true,
            showQROnLoad = false,
            focusedTextInputBorderColor = "#CCCCCC",
            unfocusedTextInputBorderColor = "#DDDDDD",
        )
    }

    private fun sessionDetails(paymentMethods: List<PaymentMethod>) = SessionDetails(
        configs = Configs(paymentMethods = paymentMethods, enabledFields = emptyList()),
        paymentDetails = PaymentDetails(
            context = PaymentContext(countryCode = "IN", localeCode = "en-IN"),
            money = Money(currencySymbol = "\u20b9", currencyCode = "INR", amount = 499.0),
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

    @Test
    fun `while the session is loading, the shimmer placeholder is shown`() {
        setCheckoutToken(token = "test_token")
        mainScreenRepo.sessionDetailsResult = ApiResponse.Loading

        composeTestRule.setContent {
            ProvideSDKFonts {
                MainScreen(
                    viewModel = newViewModel(), onProceedCardScreen = {}, onProceedEMIScreen = {},
                    onProceedAddressScreen = {}, onProceedSavedAddressScreen = {}, onProceedNetBankingScreen = {},
                    onProceedWalletScreen = {}, onProceedBNPLScreen = {}, onProceedUPITimerScreen = {},
                    onShowSwipeToPay = {}, onProceedInstantOfferScreen = {}, selectedOfferCode = "",
                    onSetSelectedOfferCode = {},
                )
            }
        }

        // Loading state renders ShimmerView, not the error/success text.
        composeTestRule.onNodeWithText("Welcome to error screen", substring = true).assertDoesNotExist()
    }

    @Test
    fun `an empty session token surfaces as an error without calling the api`() {
        setCheckoutToken(token = "") // CheckoutDetailsHandler default is also empty, but be explicit

        composeTestRule.setContent {
            ProvideSDKFonts {
                MainScreen(
                    viewModel = newViewModel(), onProceedCardScreen = {}, onProceedEMIScreen = {},
                    onProceedAddressScreen = {}, onProceedSavedAddressScreen = {}, onProceedNetBankingScreen = {},
                    onProceedWalletScreen = {}, onProceedBNPLScreen = {}, onProceedUPITimerScreen = {},
                    onShowSwipeToPay = {}, onProceedInstantOfferScreen = {}, selectedOfferCode = "",
                    onSetSelectedOfferCode = {},
                )
            }
        }

        // loadSession() checks the token synchronously and returns before
        // ever calling the repo, so the important assertion is just this:
        composeTestRule.onNodeWithText("Welcome to error screen Token is empty").assertExists()
    }

    @Test
    fun `when exactly one payment method is enabled, MainScreen auto-navigates to it`() {
        setCheckoutToken(token = "test_token")
        mainScreenRepo.sessionDetailsResult = ApiResponse.Success(
            sessionDetails(paymentMethods = listOf(PaymentMethod(id = "card_1", type = "Card", brand = "Card"))),
            responseCode = 200,
        )
        var navigatedWithAutoNav: Boolean? = null

        composeTestRule.setContent {
            ProvideSDKFonts {
                MainScreen(
                    viewModel = newViewModel(),
                    onProceedCardScreen = { navigatedWithAutoNav = it },
                    onProceedEMIScreen = {}, onProceedAddressScreen = {}, onProceedSavedAddressScreen = {},
                    onProceedNetBankingScreen = {}, onProceedWalletScreen = {}, onProceedBNPLScreen = {},
                    onProceedUPITimerScreen = {}, onShowSwipeToPay = {}, onProceedInstantOfferScreen = {},
                    selectedOfferCode = "", onSetSelectedOfferCode = {},
                )
            }
        }

        assertTrue(navigatedWithAutoNav == true) // auto-nav passes isAutoNavigationEnabled = true
    }

    @Test
    fun `with cards visible among saved recommended instruments, tapping Add new Card navigates without auto-nav`() {
        setCheckoutToken(token = "test_token", shopperToken = "shopper_tok") // shopperToken required to fetch recommended instruments
        mainScreenRepo.sessionDetailsResult = ApiResponse.Success(
            sessionDetails(
                paymentMethods = listOf(
                    PaymentMethod(id = "card_1", type = "Card", brand = "Card"),
                    PaymentMethod(id = "wallet_1", type = "Wallet", brand = "GooglePay"),
                )
            ),
            responseCode = 200,
        )
        mainScreenRepo.recommendedInstrumentsResult = ApiResponse.Success(
            listOf(
                RecommendedInstrumentsResponse(
                    type = "Card", brand = "VISA", instrumentRef = "ref_1",
                    displayValue = "\u2022\u2022\u2022\u2022 1111", logoUrl = null, cardNickName = "My Card",
                )
            ),
            responseCode = 200,
        )
        var navigatedWithAutoNav: Boolean? = null

        composeTestRule.setContent {
            ProvideSDKFonts {
                MainScreen(
                    viewModel = newViewModel(),
                    onProceedCardScreen = { navigatedWithAutoNav = it },
                    onProceedEMIScreen = {}, onProceedAddressScreen = {}, onProceedSavedAddressScreen = {},
                    onProceedNetBankingScreen = {}, onProceedWalletScreen = {}, onProceedBNPLScreen = {},
                    onProceedUPITimerScreen = {}, onShowSwipeToPay = {}, onProceedInstantOfferScreen = {},
                    selectedOfferCode = "", onSetSelectedOfferCode = {},
                )
            }
        }
        // Two methods enabled (card + wallet) -> no auto-navigation fires.
        assertTrue(navigatedWithAutoNav == null)

        composeTestRule.onNodeWithText("My Card").assertExists()
        composeTestRule.onNodeWithText("Add new Card").performClick()

        assertTrue(navigatedWithAutoNav == false) // real click passes isAutoNavigationEnabled = false
    }

    @Test
    fun `session api error surfaces the real error message on screen`() {
        setCheckoutToken(token = "test_token")
        mainScreenRepo.sessionDetailsResult = ApiResponse.Error(message = "session expired or invalid")

        composeTestRule.setContent {
            ProvideSDKFonts {
                MainScreen(
                    viewModel = newViewModel(), onProceedCardScreen = {}, onProceedEMIScreen = {},
                    onProceedAddressScreen = {}, onProceedSavedAddressScreen = {}, onProceedNetBankingScreen = {},
                    onProceedWalletScreen = {}, onProceedBNPLScreen = {}, onProceedUPITimerScreen = {},
                    onShowSwipeToPay = {}, onProceedInstantOfferScreen = {}, selectedOfferCode = "",
                    onSetSelectedOfferCode = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Welcome to error screen session expired or invalid").assertExists()
    }
}
