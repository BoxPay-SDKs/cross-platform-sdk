package com.crossplatform.sdk.presentation.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.EmiMethod
import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.data.model.ProcessingFee
import com.crossplatform.sdk.fakes.FakeOtherPaymentMethodRepo
import com.crossplatform.sdk.fakes.testKoinModule
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
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
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EMIScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val otherPaymentMethodRepo = FakeOtherPaymentMethodRepo()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        startKoin { modules(testKoinModule(otherPaymentMethodRepo = otherPaymentMethodRepo)) }
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun emiPaymentMethod(bankTitle: String = "HDFC Credit Card EMI") = PaymentMethod(
        id = "pm_1", type = "Emi", brand = "Emi", title = bankTitle, logoUrl = null,
        emiMethod = EmiMethod(
            duration = 3, effectiveInterestRate = 12.0, merchantBorneInterestRate = 0.0,
            issuerTitle = "HDFC Bank", issuer = "HDFC", processingFee = ProcessingFee(amountLocaleFull = "\u20b90"),
            netAmountLocaleFull = "\u20b95,000", totalAmountLocaleFull = "\u20b95,100", emiAmountLocaleFull = "\u20b91,700",
            merchantBorneInterestAmountLocaleFull = "\u20b90", bankChargedInterestAmountLocaleFull = "\u20b9100",
            interestChargedAmountLocaleFull = "\u20b950", cardlessEmiProviderTitle = null, cardlessEmiProviderValue = null,
        ),
    )

    @Test
    fun `on success, the Content step renders the bank list under All Banks`() {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(listOf(emiPaymentMethod()), responseCode = 200)

        composeTestRule.setContent {
            ProvideSDKFonts { EMIScreen(onBackPress = {}, isAutoNavigationEnabled = false, onExitCheckout = {}) }
        }

        composeTestRule.onNodeWithText("All Banks").assertExists()
        composeTestRule.onNodeWithText("HDFC Bank").assertExists()
    }

    @Test
    fun `on error, shows the error message`() {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Error(message = "could not load EMI options")

        composeTestRule.setContent {
            ProvideSDKFonts { EMIScreen(onBackPress = {}, isAutoNavigationEnabled = false, onExitCheckout = {}) }
        }

        composeTestRule.onNodeWithText("Welcome to error screen could not load EMI options").assertExists()
    }

    @Test
    fun `clicking a bank in the Content step advances to the Tenure step`() {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(listOf(emiPaymentMethod()), responseCode = 200)

        composeTestRule.setContent {
            ProvideSDKFonts { EMIScreen(onBackPress = {}, isAutoNavigationEnabled = false, onExitCheckout = {}) }
        }
        composeTestRule.onNodeWithText("HDFC Bank").performClick()

        // SelectTenureScreen renders "<bank name> | <card type> EMI" as its heading.
        composeTestRule.onNodeWithText("HDFC Bank | Credit Card EMI").assertExists()
    }

    @Test
    fun `isAutoNavigationEnabled true immediately triggers onExitCheckout`() {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        var exited = false

        composeTestRule.setContent {
            ProvideSDKFonts { EMIScreen(onBackPress = {}, isAutoNavigationEnabled = true, onExitCheckout = { exited = true }) }
        }

        assertTrue(exited)
    }
}
