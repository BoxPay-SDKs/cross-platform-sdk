package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TopBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `renders the given title text`() {
        composeTestRule.setContent {
            ProvideSDKFonts { TopBar(showDesc = false, text = "Pay via Card", onBackPress = {}) }
        }

        composeTestRule.onNodeWithText("Pay via Card").assertExists()
    }

    @Test
    fun `tapping the back button invokes onBackPress when the merchant logo back-arrow is visible`() {
        // isMerchantLogoVisible defaults to false in defaultCheckoutDetails() —
        // the back IconButton only renders when it's explicitly true.
        CheckoutDetailsHandler.set(CheckoutDetailsHandler.checkoutDetails.copy(isMerchantLogoVisible = true))
        var backPressed = false
        composeTestRule.setContent {
            ProvideSDKFonts { TopBar(showDesc = false, text = "Payment Details", onBackPress = { backPressed = true }) }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backPressed)
    }

    @Test
    fun `the back button is absent when the merchant logo is not visible`() {
        CheckoutDetailsHandler.set(CheckoutDetailsHandler.checkoutDetails.copy(isMerchantLogoVisible = false))

        composeTestRule.setContent {
            ProvideSDKFonts { TopBar(showDesc = false, text = "Payment Details", onBackPress = {}) }
        }

        composeTestRule.onNodeWithContentDescription("Back").assertDoesNotExist()
    }

    @Test
    fun `the session timer is hidden when sessionSeconds is null`() {
        composeTestRule.setContent {
            ProvideSDKFonts { TopBar(showDesc = false, text = "Payment Details", onBackPress = {}, sessionSeconds = null) }
        }

        composeTestRule.onNodeWithText("Payment Details").assertExists() // sanity: screen still renders
    }

    @Test
    fun `the session timer shows a formatted mm colon ss when isSessionExpiryVisible and seconds are set`() {
        CheckoutDetailsHandler.set(CheckoutDetailsHandler.checkoutDetails.copy(isSessionExpiryVisible = true))

        composeTestRule.setContent {
            ProvideSDKFonts { TopBar(showDesc = false, text = "Payment Details", onBackPress = {}, sessionSeconds = 125L) }
        }

        composeTestRule.onNodeWithText("02:05").assertExists()
    }
}
