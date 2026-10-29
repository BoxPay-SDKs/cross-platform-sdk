package com.crossplatform.sdk.presentation.screens

import com.crossplatform.sdk.fakes.initializeKamelApplicationContext

import androidx.test.core.app.ApplicationProvider

import androidx.compose.ui.platform.LocalInspectionMode

import androidx.compose.runtime.CompositionLocalProvider

import org.jetbrains.compose.resources.PreviewContextConfigurationEffect

import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
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
        duration = duration, percent = 12.0, amount = amount, totalAmount = "₹5,100",
        discount = "₹0", interestCharged = "₹50", noCostApplied = false, lowCostApplied = false,
        processingFee = "₹0", code = code, netAmount = "₹5,000",
    )

    private fun bank(name: String, emiList: List<Emi>) = Bank(
        iconUrl = "", name = name, percent = 12.0, noCostApplied = false, lowCostApplied = false,
        emiList = emiList, cardLessEmiValue = "", issuerBrand = "HDFC",
    )

    @Test
    fun `shows the bank name and card type in the header`() {
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
                SelectTenureScreen(
                    selectedBank = bank("HDFC Bank", emiList = listOf(emi(3, "₹1,700"))),
                    cardType = "Credit Card",
                    selectedEmi = Pair(null, null),
                    buttonColor = "#000000", buttonTextColor = "#FFFFFF",
                    onClickRadio = { _, _, _ -> }, onProceed = { _, _, _, _, _ -> },
                    currencySymbol = "₹", ctaBorderRadius = 8,
                )
            }
        }

        composeTestRule.onNodeWithText("HDFC Bank | Credit Card EMI").assertExists()
    }

    @Test
    fun `renders one row per emi tenure option`() {
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
                SelectTenureScreen(
                    selectedBank = bank("HDFC Bank", emiList = listOf(emi(3, "1,700"), emi(6, "900"))),
                    cardType = "Credit Card",
                    selectedEmi = Pair(null, null),
                    buttonColor = "#000000", buttonTextColor = "#FFFFFF",
                    onClickRadio = { _, _, _ -> }, onProceed = { _, _, _, _, _ -> },
                    currencySymbol = "INR", ctaBorderRadius = 8,
                )
            }
        }

        // SelectTenureScreen.kt builds this row's text by appending "$month months x ",
        // then currencySymbol, then amount (no space before the digits), and - since
        // selectedEmi is (null, null) here, so every row is unselected - it ALSO appends
        // " | @<percent>% p.a." afterwards (percent=12.0 -> "12"). onNodeWithText without
        // substring=true requires an exact match against the WHOLE text, so match by
        // substring instead of trying to spell out the entire trailing percent text.
        composeTestRule.onNodeWithText("3 months x INR1,700", substring = true).assertExists()
        composeTestRule.onNodeWithText("6 months x INR900", substring = true).assertExists()
    }

    @Test
    fun `tapping an emi row invokes onClickRadio with its duration, amount, and code`() {
        var clicked: Triple<Int, String, String?>? = null
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
                SelectTenureScreen(
                    selectedBank = bank("HDFC Bank", emiList = listOf(emi(3, "1,700", code = "CODE3"))),
                    cardType = "Credit Card",
                    selectedEmi = Pair(null, null),
                    buttonColor = "#000000", buttonTextColor = "#FFFFFF",
                    onClickRadio = { duration, amount, code -> clicked = Triple(duration, amount, code) },
                    onProceed = { _, _, _, _, _ -> },
                    currencySymbol = "INR", ctaBorderRadius = 8,
                )
            }
        }

        // Same no-space concatenation, same trailing " | @12% p.a." text, same reason for
        // substring=true as above.
        composeTestRule.onNodeWithText("3 months x INR1,700", substring = true).performClick()

        assertEquals(Triple(3, "1,700", "CODE3"), clicked)
    }

    @Test
    fun `when an emi is already selected, its Proceed action fires onProceed with that emi's terms`() {
        var proceeded: List<Any?>? = null
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
                SelectTenureScreen(
                    selectedBank = bank("HDFC Bank", emiList = listOf(emi(3, "1,700"))),
                    cardType = "Credit Card",
                    selectedEmi = Pair(3, "1,700"), // pre-selected -> Proceed button renders for this row
                    buttonColor = "#000000", buttonTextColor = "#FFFFFF",
                    onClickRadio = { _, _, _ -> },
                    onProceed = { percent, lowCost, noCost, discount, netAmount ->
                        proceeded = listOf(percent, lowCost, noCost, discount, netAmount)
                    },
                    currencySymbol = "₹", ctaBorderRadius = 8,
                )
            }
        }

        composeTestRule.onNodeWithText("Proceed to Enter Card Details", substring = true).performClick()

        assertEquals(listOf(12.0, false, false, "₹0", "₹5,000"), proceeded)
    }
}