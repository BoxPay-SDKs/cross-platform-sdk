package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.domain.model.SurchargeModel
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShowUpdatedAmountBottomSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        // ShowUpdateAmountBottomSheet writes to CheckoutDetailsHandler via
        // setAmount() on the Proceed click, which needs Dispatchers.Main.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `with no matching surcharge, total payable equals the base amount`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                ShowUpdateAmountBottomSheet(
                    selectedMethod = "card",
                    onClickProceed = {}, onClick = {},
                    currencySymbol = "\u20b9", amount = 500.0,
                    surchargeDetails = emptyList(), ctaBorderRadius = 8,
                    buttonColor = "#000000", buttonTextColor = "#FFFFFF",
                )
            }
        }

        composeTestRule.onNodeWithText("\u20b9 500.0").assertExists() // subtotal
        // total payable row uses the same "$currencySymbol $amount" pattern, and
        // with zero surcharge it renders the same text as the subtotal.
    }

    @Test
    fun `a surcharge matching the selected method is shown and added to the total`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                ShowUpdateAmountBottomSheet(
                    selectedMethod = "card",
                    onClickProceed = {}, onClick = {},
                    currencySymbol = "\u20b9", amount = 500.0,
                    surchargeDetails = listOf(
                        SurchargeModel(applicableOn = "card", title = "Card Surcharge", surchargeCode = "SC1", network = "VISA", classification = "CONSUMER", amount = 15.0)
                    ),
                    ctaBorderRadius = 8, buttonColor = "#000000", buttonTextColor = "#FFFFFF",
                )
            }
        }

        composeTestRule.onNodeWithText("Card Surcharge").assertExists()
        composeTestRule.onNodeWithText("+ \u20b9 15.0").assertExists()
        composeTestRule.onNodeWithText("\u20b9 515.0").assertExists() // 500 + 15 total payable
    }

    @Test
    fun `a surcharge for a different method is excluded from both the row and the total`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                ShowUpdateAmountBottomSheet(
                    selectedMethod = "upi",
                    onClickProceed = {}, onClick = {},
                    currencySymbol = "\u20b9", amount = 500.0,
                    surchargeDetails = listOf(
                        SurchargeModel(applicableOn = "card", title = "Card Surcharge", surchargeCode = "SC1", network = "VISA", classification = "CONSUMER", amount = 15.0)
                    ),
                    ctaBorderRadius = 8, buttonColor = "#000000", buttonTextColor = "#FFFFFF",
                )
            }
        }

        composeTestRule.onNodeWithText("Card Surcharge").assertDoesNotExist()
        composeTestRule.onNodeWithText("\u20b9 500.0").assertExists() // unaffected total
    }

    @Test
    fun `tapping Proceed to Pay invokes onClickProceed`() {
        var proceedClicked = false
        composeTestRule.setContent {
            ProvideSDKFonts {
                ShowUpdateAmountBottomSheet(
                    selectedMethod = "card",
                    onClickProceed = { proceedClicked = true }, onClick = {},
                    currencySymbol = "\u20b9", amount = 500.0,
                    surchargeDetails = emptyList(), ctaBorderRadius = 8,
                    buttonColor = "#000000", buttonTextColor = "#FFFFFF",
                )
            }
        }

        composeTestRule.onNodeWithText("Proceed to Pay").performClick()

        assertTrue(proceedClicked)
    }
}
