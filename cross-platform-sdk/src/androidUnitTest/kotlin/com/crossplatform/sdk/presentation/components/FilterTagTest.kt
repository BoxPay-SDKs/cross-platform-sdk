package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FilterTagTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `displays exactly the text it was given`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                FilterTag(modifier = Modifier, text = "Instant Discount")
            }
        }

        composeTestRule.onNodeWithText("Instant Discount").assertExists()
    }

    @Test
    fun `renders an empty tag without crashing when text is blank`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                FilterTag(modifier = Modifier, text = "")
            }
        }

        composeTestRule.onNodeWithText("").assertExists()
    }
}
