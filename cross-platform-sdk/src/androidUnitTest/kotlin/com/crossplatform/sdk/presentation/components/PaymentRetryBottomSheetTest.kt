package com.crossplatform.sdk.presentation.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
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

/**
 * Deliberately does not assert on the automatic timeout ->
 * `onProceedForward()` transition, since that depends on
 * `LaunchedEffect`'s `delay(1000)` loop interacting with Compose's test
 * frame clock in a way this suite hasn't verified. What's covered here
 * (initial render, explicit tap-to-cancel) doesn't depend on that timing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaymentRetryBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the retry title, description and initial countdown text`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                PaymentRetryBottomSheet(
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    totalSeconds = 5,
                    onTimeout = {},
                    buttonColor = "#000000",
                    buttonTextColor = "#FFFFFF",
                    onProceedForward = {},
                    ctaBorderRadius = 8,
                )
            }
        }

        composeTestRule.onNodeWithText("Retrying your payment securely").assertExists()
        composeTestRule.onNodeWithText("Retrying in 5s \u2013 Tap to cancel").assertExists()
    }

    @Test
    fun `tapping the countdown box invokes onTimeout`() {
        var timedOut = false
        composeTestRule.setContent {
            ProvideSDKFonts {
                PaymentRetryBottomSheet(
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    totalSeconds = 5,
                    onTimeout = { timedOut = true },
                    buttonColor = "#000000",
                    buttonTextColor = "#FFFFFF",
                    onProceedForward = {},
                    ctaBorderRadius = 8,
                )
            }
        }

        composeTestRule.onNodeWithText("Retrying in 5s \u2013 Tap to cancel").performClick()

        assertTrue(timedOut)
    }
}
