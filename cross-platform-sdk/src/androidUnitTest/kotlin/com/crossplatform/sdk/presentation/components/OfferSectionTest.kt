package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.domain.model.OfferItem
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfferSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun offer(code: String) = OfferItem(
        code = code, description = "Save on $code", terms = "T&C apply", discountType = "Percentage",
        discountPercent = 10.0, applicableOn = "", discountAmount = 50.0, currencySymbol = "\u20b9", expiryDate = null,
    )

    @Test
    fun `a single offer renders the SingleOfferCard with an Apply action`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                OfferSection(
                    offers = listOf(offer("SAVE10")), selectedCode = "", themeColor = Color.Blue,
                    onApply = {}, onRemove = {}, onViewAll = {},
                )
            }
        }

        composeTestRule.onNodeWithText("SAVE10").assertExists()
        composeTestRule.onNodeWithText("Apply").assertExists()
    }

    @Test
    fun `a single applied offer shows the savings banner and a Remove action`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                OfferSection(
                    offers = listOf(offer("SAVE10")), selectedCode = "SAVE10", themeColor = Color.Blue,
                    onApply = {}, onRemove = {}, onViewAll = {},
                )
            }
        }

        composeTestRule.onNodeWithText("SAVE10 applied!").assertExists()
        composeTestRule.onNodeWithText("Remove").assertExists()
        composeTestRule.onNodeWithText("You saved").assertExists()
    }

    @Test
    fun `tapping Apply on a single offer invokes onApply with that offer`() {
        var appliedCode: String? = null
        composeTestRule.setContent {
            ProvideSDKFonts {
                OfferSection(
                    offers = listOf(offer("SAVE10")), selectedCode = "", themeColor = Color.Blue,
                    onApply = { appliedCode = it.code }, onRemove = {}, onViewAll = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Apply").performClick()

        assertEquals("SAVE10", appliedCode)
    }

    @Test
    fun `more than one offer renders the MultiOfferCard with an offer count`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                OfferSection(
                    offers = listOf(offer("SAVE10"), offer("WELCOME50")), selectedCode = "", themeColor = Color.Blue,
                    onApply = {}, onRemove = {}, onViewAll = {},
                )
            }
        }

        composeTestRule.onNodeWithText("2 offers available").assertExists()
        composeTestRule.onNodeWithText("Tap to apply").assertExists()
    }

    @Test
    fun `more than MAX_VISIBLE_CHIPS offers shows a overflow chip`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                OfferSection(
                    offers = listOf(offer("A"), offer("B"), offer("C"), offer("D"), offer("E")),
                    selectedCode = "", themeColor = Color.Blue, onApply = {}, onRemove = {}, onViewAll = {},
                )
            }
        }

        composeTestRule.onNodeWithText("+2 more").assertExists()
    }

    @Test
    fun `tapping View all invokes onViewAll`() {
        var viewAllClicked = false
        composeTestRule.setContent {
            ProvideSDKFonts {
                OfferSection(
                    offers = listOf(offer("A"), offer("B")), selectedCode = "", themeColor = Color.Blue,
                    onApply = {}, onRemove = {}, onViewAll = { viewAllClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithText("View all >").performClick()

        assertTrue(viewAllClicked)
    }
}
