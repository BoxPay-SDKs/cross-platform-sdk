package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SavedCardComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun savedCard(id: String, nickName: String, displayValue: String) = SelectedPaymentMethod(
        type = "Card", id = id, displayName = nickName, displayValue = displayValue,
        imageUrl = "", instrumentType = "card", isLastUsed = false, isSelected = false,
    )

    @Test
    fun `renders the nickname and masked number for each saved card`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                SavedCardComponent(
                    savedCards = listOf(savedCard(id = "ref_1", nickName = "My Shopping Card", displayValue = "\u2022\u2022\u2022\u2022 1111")),
                    onProceedForward = { _, _ -> },
                    onClickAddCard = {},
                    buttonTextColor = "#FFFFFF",
                    buttonColor = "#000000",
                    currencySymbol = "\u20b9",
                    amount = 499.0,
                    ctaBorderRadius = 8,
                    isSICheckboxChecked = false,
                    isSICheckboxEnabled = false,
                    onClickDeleteCard = { _, _ -> },
                    onClickRadio = {},
                )
            }
        }

        composeTestRule.onNodeWithText("My Shopping Card").assertExists()
        composeTestRule.onNodeWithText("\u2022\u2022\u2022\u2022 1111").assertExists()
    }

    @Test
    fun `tapping Add new Card invokes onClickAddCard`() {
        var addCardClicked = false
        composeTestRule.setContent {
            ProvideSDKFonts {
                SavedCardComponent(
                    savedCards = emptyList(),
                    onProceedForward = { _, _ -> },
                    onClickAddCard = { addCardClicked = true },
                    buttonTextColor = "#FFFFFF",
                    buttonColor = "#000000",
                    currencySymbol = "\u20b9",
                    amount = 499.0,
                    ctaBorderRadius = 8,
                    isSICheckboxChecked = false,
                    isSICheckboxEnabled = false,
                    onClickDeleteCard = { _, _ -> },
                    onClickRadio = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Add new Card").performClick()

        assertTrue(addCardClicked)
    }

    @Test
    fun `renders one row per saved card`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                SavedCardComponent(
                    savedCards = listOf(
                        savedCard(id = "ref_1", nickName = "Shopping Card", displayValue = "\u2022\u2022\u2022\u2022 1111"),
                        savedCard(id = "ref_2", nickName = "Travel Card", displayValue = "\u2022\u2022\u2022\u2022 2222"),
                    ),
                    onProceedForward = { _, _ -> },
                    onClickAddCard = {},
                    buttonTextColor = "#FFFFFF",
                    buttonColor = "#000000",
                    currencySymbol = "\u20b9",
                    amount = 499.0,
                    ctaBorderRadius = 8,
                    isSICheckboxChecked = false,
                    isSICheckboxEnabled = false,
                    onClickDeleteCard = { _, _ -> },
                    onClickRadio = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Shopping Card").assertExists()
        composeTestRule.onNodeWithText("Travel Card").assertExists()
    }
}
