package com.crossplatform.sdk.presentation.components

import com.crossplatform.sdk.fakes.initializeKamelApplicationContext

import androidx.test.core.app.ApplicationProvider

import androidx.compose.ui.platform.LocalInspectionMode

import androidx.compose.runtime.CompositionLocalProvider

import org.jetbrains.compose.resources.PreviewContextConfigurationEffect

import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExitCheckoutConfirmationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the confirmation title and body text`() {
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
                ExitCheckoutConfirmation(onConfirmExit = {}, onStay = {}, buttonColor = "#000000", buttonTextColor = "#FFFFFF", ctaBorderRadius = 8)
            }
        }

        composeTestRule.onNodeWithText("Exit checkout?").assertExists()
        composeTestRule.onNodeWithText("Your payment hasn't been completed yet. If you exit now, your progress will be lost.").assertExists()
    }

    @Test
    fun `tapping Exit checkout eventually invokes onConfirmExit`() {
        var confirmed = false
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
                ExitCheckoutConfirmation(onConfirmExit = { confirmed = true }, onStay = {}, buttonColor = "#000000", buttonTextColor = "#FFFFFF", ctaBorderRadius = 8)
            }
        }

        composeTestRule.onNodeWithText("Exit checkout").performClick()
        composeTestRule.waitForIdle() // the callback fires after the bottom sheet's hide animation completes

        assertTrue(confirmed)
    }

    @Test
    fun `tapping Back to Checkout invokes onStay`() {
        var stayed = false
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
                ExitCheckoutConfirmation(onConfirmExit = {}, onStay = { stayed = true }, buttonColor = "#000000", buttonTextColor = "#FFFFFF", ctaBorderRadius = 8)
            }
        }

        composeTestRule.onNodeWithText("Back to Checkout", substring = true).performClick()

        assertTrue(stayed)
    }
}
