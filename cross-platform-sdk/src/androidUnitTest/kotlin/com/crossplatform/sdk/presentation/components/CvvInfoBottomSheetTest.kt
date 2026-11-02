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
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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

    // printToLog() routes through android.util.Log, which under Robolectric doesn't
    // reliably surface in Gradle's captured "Standard output" - plain println() does
    // (it's what every stack trace in this suite already shows up through), so dump the
    // tree manually instead of relying on printToLog().
    private fun printNode(node: SemanticsNode, depth: Int = 0) {
        val indent = "  ".repeat(depth)
        val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
        val hasClick = node.config.contains(SemanticsActions.OnClick)
        println("$indent[id=${node.id}] bounds=${node.boundsInWindow} text=$text hasClick=$hasClick")
        node.children.forEach { printNode(it, depth + 1) }
    }

    private fun collectAll(node: SemanticsNode): List<SemanticsNode> =
        listOf(node) + node.children.flatMap { collectAll(it) }

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

        println("=== CVV_ALL_ROOTS begin ===")
        composeTestRule.onAllNodes(isRoot()).fetchSemanticsNodes().forEachIndexed { i, root ->
            println("--- root $i ---")
            printNode(root)
        }
        println("=== CVV_ALL_ROOTS end ===")

        // Both performTouchInput{click()} (real coordinate-based touch) AND performClick()
        // (Compose test's own "call the registered action directly" API) have now failed
        // on BOTH candidate nodes - the button and the full-sheet node behind it. Compose's
        // performClick() doesn't silently no-op: it throws if no action is registered, and
        // it didn't throw, meaning an action WAS found and called each time. So the problem
        // isn't the click mechanism - it's that invoking the registered action doesn't lead
        // to `clicked` becoming true. Bypass Compose's test API entirely and call the raw
        // action reference ourselves, printing exactly what we find and what happens, so
        // there's real evidence instead of another guess.
        val allNodes = composeTestRule.onAllNodes(isRoot()).fetchSemanticsNodes()
            .flatMap { collectAll(it) }

        val clickableNodes = allNodes.filter { it.config.contains(SemanticsActions.OnClick) }
        println("=== CVV_RAW_INVOKE: ${clickableNodes.size} clickable node(s) found ===")
        clickableNodes.forEach { node ->
            val text = node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
            val action = node.config.getOrNull(SemanticsActions.OnClick)?.action
            println("CVV_RAW_INVOKE: id=${node.id} text=$text action=$action")
            val result = action?.invoke()
            println("CVV_RAW_INVOKE: id=${node.id} invoke() returned $result, clicked is now $clicked")
        }
        println("=== CVV_RAW_INVOKE end: clicked=$clicked ===")

        composeTestRule.waitForIdle()
        assertTrue(clicked)
    }
}
