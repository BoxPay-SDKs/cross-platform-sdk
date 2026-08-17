package com.crossplatform.sdk.presentation.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The real `WebViewScreen` composable is `expect fun` — its Android
 * `actual` wraps a real `android.webkit.WebView`, which isn't meaningfully
 * testable under Robolectric. These two helper composables from the same
 * file are plain Compose with no platform dependency, so they're covered
 * directly instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WebViewUrlBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the given url`() {
        composeTestRule.setContent {
            WebViewUrlBar(currentUrl = "https://pay.example.com/checkout")
        }

        composeTestRule.onNodeWithText("https://pay.example.com/checkout").assertExists()
    }

    @Test
    fun `shows a loading placeholder when the url is empty`() {
        composeTestRule.setContent {
            WebViewUrlBar(currentUrl = "")
        }

        composeTestRule.onNodeWithText("Loading\u2026").assertExists()
    }
}
