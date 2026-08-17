package com.crossplatform.sdk.presentation.components

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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CvvInfoBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows both the generic and Amex CVV explanations`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                CvvInfoBottomSheet(onClick = {}, buttonColor = "#000000", borderRadius = 8, buttonTextColor = "#FFFFFF")
            }
        }

        composeTestRule.onNodeWithText("Where to find CVV?").assertExists()
        composeTestRule.onNodeWithText("3 digit numeric code on the back side of card").assertExists()
        composeTestRule.onNodeWithText("4 digit numeric code on the front side of the card, just above the card number").assertExists()
    }

    @Test
    fun `tapping Got it invokes onClick`() {
        var clicked = false
        composeTestRule.setContent {
            ProvideSDKFonts {
                CvvInfoBottomSheet(onClick = { clicked = true }, buttonColor = "#000000", borderRadius = 8, buttonTextColor = "#FFFFFF")
            }
        }

        composeTestRule.onNodeWithText("Got it!", substring = true).performClick()

        assertTrue(clicked)
    }
}
