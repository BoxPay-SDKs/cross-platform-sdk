package com.crossplatform.sdk.presentation.components

import com.crossplatform.sdk.fakes.initializeKamelApplicationContext

import androidx.test.core.app.ApplicationProvider

import androidx.compose.ui.platform.LocalInspectionMode

import androidx.compose.runtime.CompositionLocalProvider

import org.jetbrains.compose.resources.PreviewContextConfigurationEffect

import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
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

    private fun printNode(node: SemanticsNode, depth: Int = 0) {
        val indent = "  ".repeat(depth)
        val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
        val hasClick = node.config.contains(SemanticsActions.OnClick)
        println("$indent[id=${node.id}] bounds=${node.boundsInWindow} text=$text hasClick=$hasClick")
        node.children.forEach { printNode(it, depth + 1) }
    }

    @Test
    fun `shows both the generic and Amex CVV explanations`() {
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
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
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
                CvvInfoBottomSheet(onClick = { clicked = true }, buttonColor = "#000000", borderRadius = 8, buttonTextColor = "#FFFFFF")
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasText("Got it!", substring = true) and hasClickAction())
            .performClick()

        assertTrue(clicked)
    }
}