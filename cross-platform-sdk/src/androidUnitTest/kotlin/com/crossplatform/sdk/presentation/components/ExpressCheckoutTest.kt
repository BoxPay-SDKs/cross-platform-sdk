package com.crossplatform.sdk.presentation.components

import com.crossplatform.sdk.fakes.initializeKamelApplicationContext

import androidx.test.core.app.ApplicationProvider

import androidx.compose.ui.platform.LocalInspectionMode

import androidx.compose.runtime.CompositionLocalProvider

import org.jetbrains.compose.resources.PreviewContextConfigurationEffect

import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.domain.handler.GooglePayExpressCheckoutConfig
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/*
 * NOTE: `ExpressCheckout` does not take a payment handler — the caller
 * decides which buttons to show via plain `show___` booleans and supplies
 * individual onClick callbacks. These tests exercise that contract directly
 * rather than through a handler fake.
 */

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExpressCheckoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val config = GooglePayExpressCheckoutConfig(
        gateway = "example", merchantId = "merchant_1", merchantName = "Test Merchant",
        allowedPaymentMethods = emptyList(), siteReference = "site_1",
    )

    @Test
    fun `none of the buttons render when no express method is available`() {
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
                ExpressCheckout(
                    config = config,
                    showApplePay = false, showGooglePay = false, showRevolutPay = false, showAffirm = false,
                    onClickRevolut = {}, onClickGooglePay = {}, onClickApplePay = {}, onClickAffirm = {},
                )
            }
        }

        composeTestRule.onAllNodesWithText("Pay").assertCountEquals(0)
    }

    @Test
    fun `only Google Pay shows when only it is available, and clicking it fires the callback`() {
        var googlePayClicked = false
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
                ExpressCheckout(
                    config = config,
                    showApplePay = false, showGooglePay = true, showRevolutPay = false, showAffirm = false,
                    onClickRevolut = {}, onClickGooglePay = { googlePayClicked = true }, onClickApplePay = {}, onClickAffirm = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("google pay ").assertExists()
        composeTestRule.onNodeWithContentDescription("apple pay ").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("google pay ").performClick()
        assertTrue(googlePayClicked)
    }

    @Test
    fun `all three buttons show when all are available`() {
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
                ExpressCheckout(
                    config = config,
                    showApplePay = true, showGooglePay = true, showRevolutPay = true, showAffirm = false,
                    onClickRevolut = {}, onClickGooglePay = {}, onClickApplePay = {}, onClickAffirm = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Google Pay").assertExists()
        composeTestRule.onNodeWithContentDescription("Apple Pay").assertExists()
        composeTestRule.onNodeWithContentDescription("Revolut Pay").assertExists()
    }

    @Test
    fun `tapping Apple Pay fires its own callback, not Google Pay's`() {
        var applePayClicked = false
        var googlePayClicked = false
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
                ExpressCheckout(
                    config = config,
                    showApplePay = true, showGooglePay = false, showRevolutPay = false, showAffirm = false,
                    onClickRevolut = {}, onClickGooglePay = { googlePayClicked = true }, onClickApplePay = { applePayClicked = true }, onClickAffirm = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("apple pay ").performClick()

        assertTrue(applePayClicked)
        assertTrue(!googlePayClicked)
    }
}
