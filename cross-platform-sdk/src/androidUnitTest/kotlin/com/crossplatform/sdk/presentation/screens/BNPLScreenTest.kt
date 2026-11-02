package com.crossplatform.sdk.presentation.screens

import com.crossplatform.sdk.fakes.initializeKamelApplicationContext

import androidx.test.core.app.ApplicationProvider

import androidx.compose.ui.platform.LocalInspectionMode

import androidx.compose.runtime.CompositionLocalProvider

import org.jetbrains.compose.resources.PreviewContextConfigurationEffect

import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
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
            listOf(PaymentMethod(id = "simpl", type = "BuyNowPayLater", brand = "BuyNowPayLater", title = "Simpl")),
            responseCode = 200,
        )

        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) { BNPLScreen(onBackPress = {}, isAutoNavigationEnabled = false, onExitCheckout = {}) }
        }

        composeTestRule.onNodeWithText("Simpl").assertExists()
    }

    @Test
    fun `on error, the real screen shows the error message`() {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Error(message = "could not load BNPL providers")

        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) { BNPLScreen(onBackPress = {}, isAutoNavigationEnabled = false, onExitCheckout = {}) }
        }

        composeTestRule.onNodeWithText("Welcome to error screen could not load BNPL providers").assertExists()
    }

    @Test
    fun `isAutoNavigationEnabled true immediately triggers onExitCheckout`() {
        otherPaymentMethodRepo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        var exited = false

        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) { BNPLScreen(onBackPress = {}, isAutoNavigationEnabled = true, onExitCheckout = { exited = true }) }
        }

        assertTrue(exited)
    }
}
