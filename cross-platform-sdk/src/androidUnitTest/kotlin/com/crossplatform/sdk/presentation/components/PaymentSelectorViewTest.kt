package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_netbanking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaymentSelectorViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun provider(id: String, name: String) = SelectedPaymentMethod(
        type = "NetBanking", id = id, displayName = name, displayValue = name,
        imageUrl = "", instrumentType = "netbanking", isLastUsed = false, isSelected = false,
    )

    @Test
    fun `renders one row per provider in the list`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                PaymentSelectorView(
                    providerList = listOf(provider("hdfc", "HDFC Bank"), provider("sbi", "SBI")),
                    onProceedForward = { _, _, _ -> },
                    buttonTextColor = "#FFFFFF",
                    buttonColor = "#000000",
                    drawableResource = Res.drawable.ic_netbanking,
                    onClickRadio = {},
                    currencySymbol = "\u20b9",
                    amount = 499.0,
                    ctaBorderRadius = 8,
                    selectedId = "",
                )
            }
        }

        composeTestRule.onNodeWithText("HDFC Bank").assertExists()
        composeTestRule.onNodeWithText("SBI").assertExists()
    }

    @Test
    fun `renders nothing when the provider list is empty`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                PaymentSelectorView(
                    providerList = emptyList(),
                    onProceedForward = { _, _, _ -> },
                    buttonTextColor = "#FFFFFF",
                    buttonColor = "#000000",
                    drawableResource = Res.drawable.ic_netbanking,
                    onClickRadio = {},
                    currencySymbol = "\u20b9",
                    amount = 499.0,
                    ctaBorderRadius = 8,
                    selectedId = "",
                )
            }
        }

        composeTestRule.onNodeWithText("HDFC Bank").assertDoesNotExist()
    }
}
