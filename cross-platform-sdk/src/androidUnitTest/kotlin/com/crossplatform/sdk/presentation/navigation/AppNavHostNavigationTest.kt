package com.crossplatform.sdk.presentation.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.CheckoutTheme
import com.crossplatform.sdk.data.model.Configs
import com.crossplatform.sdk.data.model.DeliveryAddress
import com.crossplatform.sdk.data.model.MerchantDetails
import com.crossplatform.sdk.data.model.Money
import com.crossplatform.sdk.data.model.PaymentContext
import com.crossplatform.sdk.data.model.PaymentDetails
import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.data.model.SessionDetails
import com.crossplatform.sdk.data.model.Shopper
import com.crossplatform.sdk.fakes.FakeMainScreenRepo
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The real end-to-end flow: set a session token exactly like
 * `BoxPayCommonCheckout` does at SDK entry, have the fake session API
 * respond, and let the real [AppNavHost] + [com.crossplatform.sdk.presentation.viewmodel.MainScreenViewModel]
 * decide what to show. This is one level above [MainScreenScreenTest] —
 * that test proves `MainScreen`'s own callbacks fire correctly in
 * isolation; this test proves those callbacks are actually wired to real
 * `NavController.navigate(...)` calls that change what's on screen.
 *
 * Same `loadCountryData()` asset caveat as `MainScreenScreenTest` applies
 * here — see that file's class doc for details.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppNavHostNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mainScreenRepo = FakeMainScreenRepo()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mainScreenRepo.surchargeResult = ApiResponse.Error(message = "no surcharge")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun sessionWithOnlyCardEnabled() = SessionDetails(
        configs = Configs(
            paymentMethods = listOf(PaymentMethod(id = "card_1", type = "Card", brand = "Card")),
            enabledFields = emptyList(),
        ),
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
    fun `setting the token and getting a single-method session navigates all the way to the Card screen title`() {
        mainScreenRepo.sessionDetailsResult = ApiResponse.Success(sessionWithOnlyCardEnabled(), responseCode = 200)

        composeTestRule.setContent {
            TestBoxPayCheckout(token = "test_token", mainScreenRepo = mainScreenRepo)
        }

        // MainScreen's own auto-navigation LaunchedEffect fires
        // onProceedCardScreen(true) when card is the only enabled method;
        // AppNavHost's composable{} for that route calls
        // navController.navigate(...), and the top bar title map resolves
        // Routes.CardScreen.route -> "Pay via Card". Seeing that title is
        // proof the whole chain — token -> session -> visibility -> click ->
        // navigate — actually worked, not just that a callback fired.
        composeTestRule.onNodeWithText("Pay via Card").assertExists()
    }

    @Test
    fun `an invalid empty token never gets past the main screen`() {
        mainScreenRepo.sessionDetailsResult = ApiResponse.Success(sessionWithOnlyCardEnabled(), responseCode = 200)

        composeTestRule.setContent {
            TestBoxPayCheckout(token = "")
        }

        composeTestRule.onNodeWithText("Pay via Card").assertDoesNotExist()
        composeTestRule.onNodeWithText("Welcome to error screen Token is empty").assertExists()
    }
}
