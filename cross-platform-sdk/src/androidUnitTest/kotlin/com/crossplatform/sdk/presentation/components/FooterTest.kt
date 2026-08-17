package com.crossplatform.sdk.presentation.components

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
class FooterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the secured-by and terms text`() {
        composeTestRule.setContent {
            ProvideSDKFonts { Footer() }
        }

        composeTestRule.onNodeWithText("Secured by").assertExists()
        composeTestRule.onNodeWithText("\u00b7 Terms & Conditions").assertExists()
    }
}
