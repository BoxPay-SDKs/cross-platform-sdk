package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.domain.model.MainScreenModel.OrderItemUiModel
import com.crossplatform.sdk.domain.model.SurchargeModel
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OrderDetailsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        totalAmount: Double = 499.0,
        items: List<OrderItemUiModel> = listOf(OrderItemUiModel(imageUrl = null, imageTitle = "Blue T-Shirt", imageQty = 2, amount = 250.0)),
        subTotalAmount: Double = 0.0,
        shippingAmount: Double = 0.0,
        taxAmount: Double = 0.0,
        surchargeDetails: List<SurchargeModel> = emptyList(),
        selectedPaymentMethod: String = "card",
    ) {
        composeTestRule.setContent {
            ProvideSDKFonts {
                OrderDetails(
                    totalAmount = totalAmount,
                    itemsArray = items,
                    subTotalAmount = subTotalAmount,
                    shippingAmount = shippingAmount,
                    taxAmount = taxAmount,
                    surchargeDetails = surchargeDetails,
                    selectedPaymentMethod = selectedPaymentMethod,
                    currencySymbol = "\u20b9",
                )
            }
        }
    }

    @Test
    fun `starts collapsed showing only the header and total`() {
        setContent(totalAmount = 499.0)

        composeTestRule.onNodeWithText("Price Details").assertExists()
        composeTestRule.onNodeWithText("\u20b9 499.0").assertExists()
        // Item rows only render once expanded.
        composeTestRule.onNodeWithText("Blue T-Shirt").assertDoesNotExist()
    }

    @Test
    fun `tapping the collapsed header expands it and reveals the item list`() {
        setContent()

        composeTestRule.onNodeWithText("Price Details").performClick()

        composeTestRule.onNodeWithText("Blue T-Shirt").assertExists()
        composeTestRule.onNodeWithText("Qty: 2").assertExists()
    }

    @Test
    fun `tapping the expanded header collapses it again`() {
        setContent()
        composeTestRule.onNodeWithText("Price Details").performClick() // expand

        composeTestRule.onNodeWithText("Price Details").performClick() // collapse

        composeTestRule.onNodeWithText("Blue T-Shirt").assertDoesNotExist()
    }

    @Test
    fun `zero-value summary rows are hidden while non-zero ones render`() {
        setContent(subTotalAmount = 500.0, taxAmount = 0.0, shippingAmount = 0.0)
        composeTestRule.onNodeWithText("Price Details").performClick() // expand

        composeTestRule.onNodeWithText("Subtotal").assertExists()
        composeTestRule.onNodeWithText("Taxes and Fees").assertDoesNotExist()
        composeTestRule.onNodeWithText("Shipping Amount").assertDoesNotExist()
    }

    @Test
    fun `a surcharge matching the selected payment method is shown`() {
        setContent(
            selectedPaymentMethod = "card",
            surchargeDetails = listOf(
                SurchargeModel(applicableOn = "card", title = "Card Surcharge", surchargeCode = "SC1", network = "VISA", classification = "CONSUMER", amount = 10.0)
            ),
        )
        composeTestRule.onNodeWithText("Price Details").performClick() // expand

        composeTestRule.onNodeWithText("Card Surcharge").assertExists()
    }

    @Test
    fun `a surcharge for a different payment method is filtered out`() {
        setContent(
            selectedPaymentMethod = "upi",
            surchargeDetails = listOf(
                SurchargeModel(applicableOn = "card", title = "Card Surcharge", surchargeCode = "SC1", network = "VISA", classification = "CONSUMER", amount = 10.0)
            ),
        )
        composeTestRule.onNodeWithText("Price Details").performClick() // expand

        composeTestRule.onNodeWithText("Card Surcharge").assertDoesNotExist()
    }
}
