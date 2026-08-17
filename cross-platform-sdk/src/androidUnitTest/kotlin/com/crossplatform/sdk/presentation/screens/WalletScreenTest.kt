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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WalletScreenTest {

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
    fun `on success, the real screen renders the wallet list under the All Wallet title`() {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(
            listOf(PaymentMethod(id = "gpay", type = "Wallet", brand = "Wallet", title = "Google Pay")),
            responseCode = 200,
        )

        composeTestRule.setContent {
            ProvideSDKFonts { WalletScreen(onBackPress = {}, isAutoNavigationEnabled = false, onExitCheckout = {}) }
        }

        composeTestRule.onNodeWithText("All Wallet").assertExists()
        composeTestRule.onNodeWithText("Google Pay").assertExists()
    }

    @Test
    fun `on error, the real screen shows the error message`() {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Error(message = "could not load wallets")

        composeTestRule.setContent {
            ProvideSDKFonts { WalletScreen(onBackPress = {}, isAutoNavigationEnabled = false, onExitCheckout = {}) }
        }

        composeTestRule.onNodeWithText("Welcome to error screen could not load wallets").assertExists()
    }

    @Test
    fun `isAutoNavigationEnabled true immediately triggers onExitCheckout`() {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        var exited = false

        composeTestRule.setContent {
            ProvideSDKFonts { WalletScreen(onBackPress = {}, isAutoNavigationEnabled = true, onExitCheckout = { exited = true }) }
        }

        assertTrue(exited)
    }
}
