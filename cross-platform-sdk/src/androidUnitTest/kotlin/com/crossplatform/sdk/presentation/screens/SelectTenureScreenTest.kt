package com.crossplatform.sdk.presentation.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.domain.model.Bank
import com.crossplatform.sdk.domain.model.Emi
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SelectTenureScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun emi(duration: Int, amount: String, code: String = "3M") = Emi(
        duration = duration, percent = 12.0, amount = amount, totalAmount = "\u20b95,100",
        discount = "\u20b90", interestCharged = "\u20b950", noCostApplied = false, lowCostApplied = false,
        processingFee = "\u20b90", code = code, netAmount = "\u20b95,000",
    )

    private fun bank(name: String, emiList: List<Emi>) = Bank(
        iconUrl = "", name = name, percent = 12.0, noCostApplied = false, lowCostApplied = false,
        emiList = emiList, cardLessEmiValue = "", issuerBrand = "HDFC",
    )

    @Test
    fun `shows the bank name and card type in the header`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                SelectTenureScreen(
                    selectedBank = bank("HDFC Bank", emiList = listOf(emi(3, "\u20b91,700"))),
                    cardType = "Credit Card",
                    selectedEmi = Pair(null, null),
                    buttonColor = "#000000", buttonTextColor = "#FFFFFF",
                    onClickRadio = { _, _, _ -> }, onProceed = { _, _, _, _, _ -> },
                    currencySymbol = "\u20b9", ctaBorderRadius = 8,
                )
            }
        }

        composeTestRule.onNodeWithText("HDFC Bank | Credit Card EMI").assertExists()
    }

    @Test
    fun `renders one row per emi tenure option`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                SelectTenureScreen(
                    selectedBank = bank("HDFC Bank", emiList = listOf(emi(3, "1,700"), emi(6, "900"))),
                    cardType = "Credit Card",
                    selectedEmi = Pair(null, null),
                    buttonColor = "#000000", buttonTextColor = "#FFFFFF",
                    onClickRadio = { _, _, _ -> }, onProceed = { _, _, _, _, _ -> },
                    currencySymbol = "\u20b9", ctaBorderRadius = 8,
                )
            }
        }

        composeTestRule.onNodeWithText("3 months x \u20b91,700").assertExists()
        composeTestRule.onNodeWithText("6 months x \u20b9900").assertExists()
    }

    @Test
    fun `tapping an emi row invokes onClickRadio with its duration, amount, and code`() {
        var clicked: Triple<Int, String, String?>? = null
        composeTestRule.setContent {
            ProvideSDKFonts {
                SelectTenureScreen(
                    selectedBank = bank("HDFC Bank", emiList = listOf(emi(3, "1,700", code = "CODE3"))),
                    cardType = "Credit Card",
                    selectedEmi = Pair(null, null),
                    buttonColor = "#000000", buttonTextColor = "#FFFFFF",
                    onClickRadio = { duration, amount, code -> clicked = Triple(duration, amount, code) },
                    onProceed = { _, _, _, _, _ -> },
                    currencySymbol = "\u20b9", ctaBorderRadius = 8,
                )
            }
        }

        composeTestRule.onNodeWithText("3 months x \u20b91,700").performClick()

        assertEquals(Triple(3, "1,700", "CODE3"), clicked)
    }

    @Test
    fun `when an emi is already selected, its Proceed action fires onProceed with that emi's terms`() {
        var proceeded: List<Any?>? = null
        composeTestRule.setContent {
            ProvideSDKFonts {
                SelectTenureScreen(
                    selectedBank = bank("HDFC Bank", emiList = listOf(emi(3, "1,700"))),
                    cardType = "Credit Card",
                    selectedEmi = Pair(3, "1,700"), // pre-selected -> Proceed button renders for this row
                    buttonColor = "#000000", buttonTextColor = "#FFFFFF",
                    onClickRadio = { _, _, _ -> },
                    onProceed = { percent, lowCost, noCost, discount, netAmount ->
                        proceeded = listOf(percent, lowCost, noCost, discount, netAmount)
                    },
                    currencySymbol = "\u20b9", ctaBorderRadius = 8,
                )
            }
        }

        composeTestRule.onNodeWithText("Proceed to Enter Card Details", substring = true).performClick()

        assertEquals(listOf(12.0, false, false, "\u20b90", "\u20b95,000"), proceeded)
    }
}
