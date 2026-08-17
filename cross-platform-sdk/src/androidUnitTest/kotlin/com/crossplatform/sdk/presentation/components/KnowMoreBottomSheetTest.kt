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
class KnowMoreBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the RBI guidelines heading and both info rows`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                KnowMoreBottomSheet(buttonColor = "#000000", buttonTextColor = "#FFFFFF", ctaBorderRadius = 8, onDismiss = {})
            }
        }

        composeTestRule.onNodeWithText("RBI Guidelines").assertExists()
        composeTestRule.onNodeWithText(
            "Your bank/card network will securely save your card information via tokenization if you consent for the same."
        ).assertExists()
    }

    @Test
    fun `tapping Got it invokes onDismiss`() {
        var dismissed = false
        composeTestRule.setContent {
            ProvideSDKFonts {
                KnowMoreBottomSheet(buttonColor = "#000000", buttonTextColor = "#FFFFFF", ctaBorderRadius = 8, onDismiss = { dismissed = true })
            }
        }

        composeTestRule.onNodeWithText("Got it!", substring = true).performClick()

        assertTrue(dismissed)
    }
}
