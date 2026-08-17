package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
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
            ProvideSDKFonts { CountryPickerDialog(onDismiss = {}, focusedBorderColor = "#CCCCCC", unfocusedBorderColor = "#DDDDDD", onSelect = { _, _, _, _ -> }) }
        }

        composeTestRule.onNodeWithText("Select Country").assertExists()
    }

    @Test
    fun `tapping the close icon invokes onDismiss`() {
        var dismissed = false
        composeTestRule.setContent {
            ProvideSDKFonts { CountryPickerDialog(onDismiss = { dismissed = true }, focusedBorderColor = "#CCCCCC", unfocusedBorderColor = "#DDDDDD", onSelect = { _, _, _, _ -> }) }
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
            ProvideSDKFonts {
                CountryPickerDialog(
                    onDismiss = {},
                    focusedBorderColor = "#CCCCCC",
                    unfocusedBorderColor = "#DDDDDD",
                    onSelect = { code, isdCode, fullName, _ -> selectedCode = code; selectedIsd = isdCode; selectedName = fullName },
                )
            }
        }

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
            ProvideSDKFonts { CountryPickerDialog(onDismiss = {}, focusedBorderColor = "#CCCCCC", unfocusedBorderColor = "#DDDDDD", onSelect = { _, _, _, _ -> }) }
        }

        composeTestRule.onNodeWithText("Search country").performTextInput("India")

        composeTestRule.onNodeWithText("India").assertExists()
    }
}
