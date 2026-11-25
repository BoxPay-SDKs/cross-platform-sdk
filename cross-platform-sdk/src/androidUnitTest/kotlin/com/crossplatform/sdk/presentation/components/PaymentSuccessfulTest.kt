package com.crossplatform.sdk.presentation.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.fakes.initializeKamelApplicationContext
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.jetbrains.compose.resources.PreviewContextConfigurationEffect
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaymentSuccessfulTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        CheckoutDetailsHandler.resetToDefault()
    }

    @After
    fun tearDown() {
        CheckoutDetailsHandler.resetToDefault()
        Dispatchers.resetMain()
    }

    private fun showPage(
        status: String = "SUCCESS",
        orderId: String = "ORD-1001",
        productName: String = "Latte, Croissant, Cookie",
    ) {
        CheckoutDetailsHandler.set(
            CheckoutDetailsHandler.checkoutDetails.copy(
                merchantName = "Coffee Shop",
                orderId = orderId,
                productName = productName,
                status = status,
                transactionId = "TXN-20481",
                currencyCode = "INR",
                amount = 450.0,
                isSuccessScreenVisible = true,
                buttonColor = "#000000",
                buttonTextColor = "#FFFFFF",
                ctaBorderRadius = 8,
            )
        )
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
                PaymentSuccessful(
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    dateNTime = "",
                    paymentMethod = "VISA",
                    onClick = {},
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `shows order id, status and transaction id rows`() {
        showPage()

        composeTestRule.onNodeWithText("Order ID").assertExists()
        composeTestRule.onNodeWithText("ORD-1001").assertExists()
        composeTestRule.onNodeWithText("Status").assertExists()
        composeTestRule.onNodeWithText("Success").assertExists()
        composeTestRule.onNodeWithText("TXN-20481").assertExists()
    }

    @Test
    fun `shows Processing as the status for a processing payment`() {
        showPage(status = "PROCESSING")

        composeTestRule.onNodeWithText("Processing").assertExists()
    }

    @Test
    fun `shows each product name on its own line`() {
        showPage(productName = "Latte, Croissant, Cookie")

        composeTestRule.onNodeWithText("Product Name").assertExists()
        composeTestRule.onNodeWithText("Latte,").assertExists()
        composeTestRule.onNodeWithText("Croissant,").assertExists()
        composeTestRule.onNodeWithText("Cookie").assertExists()
    }

    @Test
    fun `hides the order id row when there is no order id`() {
        showPage(orderId = "")

        composeTestRule.onNodeWithText("Order ID").assertDoesNotExist()
    }

    @Test
    fun `hides the product name row when there is no product name`() {
        showPage(productName = "")

        composeTestRule.onNodeWithText("Product Name").assertDoesNotExist()
    }
}
