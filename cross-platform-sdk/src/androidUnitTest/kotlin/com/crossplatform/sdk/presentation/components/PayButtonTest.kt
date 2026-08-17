package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI ("integration") tests for [PayButton] — the button rendered at
 * the bottom of every checkout screen. These run on the JVM against
 * Robolectric, so no device/emulator is required; they exercise the real
 * composable, real text layout, and the real [CheckoutDetailsHandler] state
 * it observes, not a mock.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PayButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the label, currency symbol and amount together`() {
        CheckoutDetailsHandler.set(CheckoutDetailsHandler.checkoutDetails.copy(ctaTextFontSize = 16))

        composeTestRule.setContent {
            ProvideSDKFonts {
                PayButton(
                    text = "Pay",
                    modifier = Modifier,
                    amount = 499.0,
                    currencySymbol = "₹",
                    isValid = true,
                    buttonTextColor = "#FFFFFF",
                )
            }
        }

        // The button composes its label out of three separately-styled spans;
        // asserting on the full merged string catches regressions in any of them.
        composeTestRule.onNodeWithText("Pay ₹ 499.0").assertExists()
    }

    @Test
    fun `omits the amount segment entirely when amount is zero`() {
        CheckoutDetailsHandler.set(CheckoutDetailsHandler.checkoutDetails.copy(ctaTextFontSize = 16))

        composeTestRule.setContent {
            ProvideSDKFonts {
                PayButton(
                    text = "Continue",
                    modifier = Modifier,
                    amount = 0.0,
                    currencySymbol = "₹",
                    isValid = true,
                    buttonTextColor = "#FFFFFF",
                )
            }
        }

        composeTestRule.onNodeWithText("Continue ₹").assertExists()
    }

    @Test
    fun `omits the currency segment entirely when currencySymbol is blank`() {
        CheckoutDetailsHandler.set(CheckoutDetailsHandler.checkoutDetails.copy(ctaTextFontSize = 16))

        composeTestRule.setContent {
            ProvideSDKFonts {
                PayButton(
                    text = "Pay",
                    modifier = Modifier,
                    amount = 250.0,
                    currencySymbol = "",
                    isValid = true,
                    buttonTextColor = "#FFFFFF",
                )
            }
        }

        // "$text " already has a trailing space, and the amount segment adds
        // its own leading space (" $amount"), so skipping the currency symbol
        // leaves a double space between the label and the amount — this is
        // the actual rendered output, not a typo.
        composeTestRule.onNodeWithText("Pay  250.0").assertExists()
    }
}
