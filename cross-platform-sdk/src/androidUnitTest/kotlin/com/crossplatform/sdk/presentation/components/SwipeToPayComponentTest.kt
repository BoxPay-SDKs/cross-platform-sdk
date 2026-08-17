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

/**
 * Covers the static/display portion of [SwipeToPayComponent] — the actual
 * swipe-to-confirm drag gesture (`draggable` + `rememberDraggableState`) is
 * not exercised here; `performTouchInput`-based drag simulation is a
 * separate, higher-effort follow-up (see TEST_PLAN.md).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SwipeToPayComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        toShowAddress: Boolean = true,
        toShowPersonal: Boolean = false,
        toShowOnChangeAddressClick: Boolean = true,
        address: String = "123 Main St, Mumbai",
        onClickChangeAddress: () -> Unit = {},
        onClickMoreOptions: () -> Unit = {},
        composeTestRule: androidx.compose.ui.test.junit4.ComposeContentTestRule,
    ) {
        composeTestRule.setContent {
            ProvideSDKFonts {
                SwipeToPayComponent(
                    buttonColor = "#000000",
                    buttonTextColor = "#FFFFFF",
                    amount = 499.0,
                    currencySymbol = "\u20b9",
                    lastUsedUpi = "jane@upi",
                    onClickMoreOptions = onClickMoreOptions,
                    onSwipeComplete = {},
                    address = address,
                    onClickChangeAddress = onClickChangeAddress,
                    toShowOnChangeAddressClick = toShowOnChangeAddressClick,
                    toShowAddress = toShowAddress,
                    toShowPersonal = toShowPersonal,
                    logoUrl = "",
                )
            }
        }
    }

    @Test
    fun `shows Shipping Address label and the address text when toShowPersonal is false`() {
        setContent(toShowAddress = true, toShowPersonal = false, address = "123 Main St, Mumbai", composeTestRule = composeTestRule)

        composeTestRule.onNodeWithText("Shipping Address").assertExists()
        composeTestRule.onNodeWithText("123 Main St, Mumbai").assertExists()
    }

    @Test
    fun `shows Personal Details label instead when toShowPersonal is true`() {
        setContent(toShowAddress = true, toShowPersonal = true, composeTestRule = composeTestRule)

        composeTestRule.onNodeWithText("Personal Details").assertExists()
        composeTestRule.onNodeWithText("Shipping Address").assertDoesNotExist()
    }

    @Test
    fun `hides the whole address section when toShowAddress is false`() {
        setContent(toShowAddress = false, composeTestRule = composeTestRule)

        composeTestRule.onNodeWithText("Shipping Address").assertDoesNotExist()
        composeTestRule.onNodeWithText("Personal Details").assertDoesNotExist()
    }

    @Test
    fun `Change is hidden when toShowOnChangeAddressClick is false`() {
        setContent(toShowAddress = true, toShowOnChangeAddressClick = false, composeTestRule = composeTestRule)

        composeTestRule.onNodeWithText("Change").assertDoesNotExist()
    }

    @Test
    fun `tapping Change invokes onClickChangeAddress`() {
        var changeClicked = false
        setContent(toShowAddress = true, toShowOnChangeAddressClick = true, onClickChangeAddress = { changeClicked = true }, composeTestRule = composeTestRule)

        composeTestRule.onNodeWithText("Change").performClick()

        assertTrue(changeClicked)
    }
}
