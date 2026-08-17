package com.crossplatform.sdk.presentation.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.PaymentMethod
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

/**
 * Renders the real [NetBankingScreen] composable — the same one your app
 * actually navigates to — with Koin wired to fakes via [testKoinModule].
 * This is a step beyond a component test: it proves the screen's
 * `koinViewModel()` resolution, its `UiState` branching (Loading/Success/
 * Error), and the real [NetBankingViewModel] all work together correctly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetBankingScreenTest {

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

    @Test
    fun `on success, the real screen renders the bank list via BankComponent`() {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(
            listOf(
                PaymentMethod(id = "hdfc", type = "NetBanking", brand = "NetBanking", title = "HDFC Bank"),
                PaymentMethod(id = "sbi", type = "NetBanking", brand = "NetBanking", title = "SBI"),
            ),
            responseCode = 200,
        )

        composeTestRule.setContent {
            ProvideSDKFonts { NetBankingScreen(onBackPress = {}, isAutoNavigationEnabled = false, onExitCheckout = {}) }
        }

        composeTestRule.onNodeWithText("All Banks").assertExists()
        composeTestRule.onNodeWithText("HDFC Bank").assertExists()
        composeTestRule.onNodeWithText("SBI").assertExists()
    }

    @Test
    fun `on error, the real screen shows the error message`() {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Error(message = "could not load banks")

        composeTestRule.setContent {
            ProvideSDKFonts { NetBankingScreen(onBackPress = {}, isAutoNavigationEnabled = false, onExitCheckout = {}) }
        }

        composeTestRule.onNodeWithText("Welcome to error screen could not load banks").assertExists()
    }

    @Test
    fun `isAutoNavigationEnabled true immediately triggers onExitCheckout`() {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        var exited = false

        composeTestRule.setContent {
            ProvideSDKFonts { NetBankingScreen(onBackPress = {}, isAutoNavigationEnabled = true, onExitCheckout = { exited = true }) }
        }

        assertTrue(exited)
    }

    @Test
    fun `an empty successful bank list shows the empty state, not a crash`() {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)

        composeTestRule.setContent {
            ProvideSDKFonts { NetBankingScreen(onBackPress = {}, isAutoNavigationEnabled = false, onExitCheckout = {}) }
        }

        composeTestRule.onNodeWithText("Oops!! No results found").assertExists()
        composeTestRule.onNodeWithText("HDFC Bank").assertDoesNotExist()
    }
}
