package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfferCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        offerCode: String = "SAVE10",
        selectedCouponCode: String = "",
        onClickApply: () -> Unit = {},
        onClickRemove: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ProvideSDKFonts {
                OfferCard(
                    modifier = Modifier,
                    selectedColor = Color.Blue,
                    offerCode = offerCode,
                    description = "Save 10% on this order",
                    discountType = "Percentage",
                    expiryDate = null,
                    applicable = "",
                    terms = "Terms apply",
                    discountAmount = null,
                    discountPercent = 10.0,
                    currencySymbol = "\u20b9",
                    selectedCouponCode = selectedCouponCode,
                    onClickApply = onClickApply,
                    onClickRemove = onClickRemove,
                )
            }
        }
    }

    @Test
    fun `shows APPLY when this offer is not the selected coupon`() {
        setContent(offerCode = "SAVE10", selectedCouponCode = "")

        composeTestRule.onNodeWithText("APPLY").assertExists()
        composeTestRule.onNodeWithText("REMOVE").assertDoesNotExist()
    }

    @Test
    fun `shows REMOVE when this offer is the currently selected coupon`() {
        setContent(offerCode = "SAVE10", selectedCouponCode = "SAVE10")

        composeTestRule.onNodeWithText("REMOVE").assertExists()
        composeTestRule.onNodeWithText("APPLY").assertDoesNotExist()
    }

    @Test
    fun `tapping APPLY invokes onClickApply and not onClickRemove`() {
        var applyClicked = false
        var removeClicked = false
        setContent(offerCode = "SAVE10", selectedCouponCode = "", onClickApply = { applyClicked = true }, onClickRemove = { removeClicked = true })

        composeTestRule.onNodeWithText("APPLY").performClick()

        assertTrue(applyClicked)
        assertFalse(removeClicked)
    }

    @Test
    fun `tapping REMOVE invokes onClickRemove and not onClickApply`() {
        var applyClicked = false
        var removeClicked = false
        setContent(offerCode = "SAVE10", selectedCouponCode = "SAVE10", onClickApply = { applyClicked = true }, onClickRemove = { removeClicked = true })

        composeTestRule.onNodeWithText("REMOVE").performClick()

        assertTrue(removeClicked)
        assertFalse(applyClicked)
    }

    @Test
    fun `terms are hidden until MORE is tapped, then LESS hides them again`() {
        setContent()

        composeTestRule.onNodeWithText("+ MORE").assertExists()

        composeTestRule.onNodeWithText("+ MORE").performClick()
        composeTestRule.onNodeWithText("- LESS").assertExists()

        composeTestRule.onNodeWithText("- LESS").performClick()
        composeTestRule.onNodeWithText("+ MORE").assertExists()
    }

    @Test
    fun `the offer code itself is always shown`() {
        setContent(offerCode = "WELCOME50")

        composeTestRule.onNodeWithText("WELCOME50").assertExists()
    }
}
