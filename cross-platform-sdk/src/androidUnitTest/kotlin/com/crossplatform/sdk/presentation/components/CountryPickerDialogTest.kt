package com.crossplatform.sdk.presentation.components

import com.crossplatform.sdk.fakes.initializeKamelApplicationContext

import androidx.test.core.app.ApplicationProvider

import androidx.compose.ui.platform.LocalInspectionMode

import androidx.compose.runtime.CompositionLocalProvider

import org.jetbrains.compose.resources.PreviewContextConfigurationEffect

import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper
import kotlin.test.assertEquals

/**
 * Same `loadCountryData()` -> `Res.readBytes("files/countryCodes.json")`
 * caveat as `MainScreenScreenTest` applies: this dialog loads real country
 * data in a `LaunchedEffect` with no injection point, so these tests assert
 * against the actual asset content (confirmed present at
 * `commonMain/composeResources/files/countryCodes.json`, e.g. "IN" ->
 * "India" / "+91").
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CountryPickerDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the header and search field`() {
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) { CountryPickerDialog(onDismiss = {}, focusedBorderColor = "#CCCCCC", unfocusedBorderColor = "#DDDDDD", onSelect = { _, _, _, _ -> }) }
        }

        // The header title and the search field's placeholder both render from the same
        // string resource (select_country_text_field = "Select Country") - there is no
        // separate "Search country" string - so exactly 2 nodes legitimately show this text.
        composeTestRule.onAllNodesWithText("Select Country").assertCountEquals(2)
    }

    @Test
    fun `tapping the close icon invokes onDismiss`() {
        var dismissed = false
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) { CountryPickerDialog(onDismiss = { dismissed = true }, focusedBorderColor = "#CCCCCC", unfocusedBorderColor = "#DDDDDD", onSelect = { _, _, _, _ -> }) }
        }

        composeTestRule.onNodeWithContentDescription("Close").performClick()

        kotlin.test.assertTrue(dismissed)
    }

    @Test
    fun `India appears in the list with its ISD code, and tapping it selects it`() {
        var selectedCode: String? = null
        var selectedIsd: String? = null
        var selectedName: String? = null
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
                CountryPickerDialog(
                    onDismiss = {},
                    focusedBorderColor = "#CCCCCC",
                    unfocusedBorderColor = "#DDDDDD",
                    onSelect = { code, isdCode, fullName, _ -> selectedCode = code; selectedIsd = isdCode; selectedName = fullName },
                )
            }
        }

        // The real cause of every earlier timeout here: this is the FULL, unfiltered,
        // alphabetically-sorted country list (~195 countries) in a LazyColumn. Compose
        // only composes items inside the visible viewport, and "India" sits around the
        // middle alphabetically - it's not virtualized into the semantics tree until
        // scrolled into view, no matter how long anything waits. (The search test below
        // never hit this because filtering shrinks the list to fit on-screen.) Scroll to
        // it directly instead of waiting for it to "appear" on its own.
        composeTestRule.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("India"))

        composeTestRule.onNodeWithText("India").assertExists()
        composeTestRule.onNodeWithText("+91").assertExists()

        composeTestRule.onNodeWithText("India").performClick()

        assertEquals("IN", selectedCode)
        assertEquals("+91", selectedIsd)
        assertEquals("India", selectedName)
    }

    @Test
    fun `typing a search query filters the country list`() {
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) { CountryPickerDialog(onDismiss = {}, focusedBorderColor = "#CCCCCC", unfocusedBorderColor = "#DDDDDD", onSelect = { _, _, _, _ -> }) }
        }

        // The placeholder text is "Select Country" (same string resource as the header),
        // and that text also matches the header's plain Text node, so disambiguate by
        // targeting only the node with a SetText action - i.e. the actual editable field.
        composeTestRule.onNode(hasText("Select Country") and hasSetTextAction())
            .performTextInput("India")

        // Same async-load race as above: the filtered list depends on loadCountryData()
        // having landed from its LaunchedEffect coroutine. Pump the main looper here too,
        // rather than relying on performTextInput's side effect to have been enough.
        composeTestRule.waitUntil(timeoutMillis = 15_000) {
            shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.onAllNodesWithText("India").fetchSemanticsNodes().isNotEmpty()
        }

        // The search field now echoes the typed "India" itself, so onNodeWithText("India")
        // would match both that field and the filtered list row - target the list row only
        // by excluding the node with a SetText action (the editable field).
        composeTestRule.onNode(hasText("India") and !hasSetTextAction()).assertExists()
    }
}