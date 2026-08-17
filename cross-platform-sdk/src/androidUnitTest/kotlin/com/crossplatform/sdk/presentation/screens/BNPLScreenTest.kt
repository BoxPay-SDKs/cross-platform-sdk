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
class BNPLScreenTest {

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
    fun `on success, the real screen renders the BNPL provider list`() {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(
            listOf(PaymentMethod(id = "simpl", type = "BNPL", brand = "BNPL", title = "Simpl")),
            responseCode = 200,
        )

        composeTestRule.setContent {
            ProvideSDKFonts { BNPLScreen(onBackPress = {}, isAutoNavigationEnabled = false, onExitCheckout = {}) }
        }

        composeTestRule.onNodeWithText("Simpl").assertExists()
    }

    @Test
    fun `on error, the real screen shows the error message`() {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Error(message = "could not load BNPL providers")

        composeTestRule.setContent {
            ProvideSDKFonts { BNPLScreen(onBackPress = {}, isAutoNavigationEnabled = false, onExitCheckout = {}) }
        }

        composeTestRule.onNodeWithText("Welcome to error screen could not load BNPL providers").assertExists()
    }

    @Test
    fun `isAutoNavigationEnabled true immediately triggers onExitCheckout`() {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        var exited = false

        composeTestRule.setContent {
            ProvideSDKFonts { BNPLScreen(onBackPress = {}, isAutoNavigationEnabled = true, onExitCheckout = { exited = true }) }
        }

        assertTrue(exited)
    }
}
