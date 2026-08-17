package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.domain.model.MainScreenModel
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MorePaymentMethodsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        methodFlags: MainScreenModel.MethodFlags,
        onNavigateToCard: () -> Unit = {},
        onNavigateToEmi: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ProvideSDKFonts {
                MorePaymentMethods(
                    methodFlags = methodFlags,
                    onNavigateToCard = onNavigateToCard,
                    onNavigateToWallet = {},
                    onNavigateToNetBanking = {},
                    onNavigateToEmi = onNavigateToEmi,
                    onNavigateToBNPL = {},
                    savedCardsList = emptyList(),
                    surchargeList = emptyList(),
                    currencySymbol = "\u20b9",
                    walletList = emptyList(),
                    netBankingList = emptyList(),
                    amount = 499.0,
                    buttonColor = "#000000",
                    buttonTextColor = "#FFFFFF",
                    ctaBorderRadius = 8,
                    onProceedForward = { _, _, _ -> },
                )
            }
        }
    }

    @Test
    fun `only shows sections whose method flag is enabled`() {
        setContent(
            methodFlags = MainScreenModel.MethodFlags(isCardsVisible = true, isEMIVisible = true, isBNPLVisible = false, isWalletVisible = false, isNetBankingVisible = false)
        )

        composeTestRule.onNodeWithText("Cards").assertExists()
        composeTestRule.onNodeWithText("EMI").assertExists()
        composeTestRule.onNodeWithText("Buy Now Pay Later").assertDoesNotExist()
    }

    @Test
    fun `cards section is hidden when a saved card already exists`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                MorePaymentMethods(
                    methodFlags = MainScreenModel.MethodFlags(isCardsVisible = true),
                    onNavigateToCard = {}, onNavigateToWallet = {}, onNavigateToNetBanking = {},
                    onNavigateToEmi = {}, onNavigateToBNPL = {},
                    savedCardsList = listOf(
                        com.crossplatform.sdk.domain.model.SelectedPaymentMethod(
                            type = "Card", id = "ref_1", displayName = "My Card", displayValue = "\u2022\u2022\u2022\u2022 1111",
                            imageUrl = "", instrumentType = "card", isLastUsed = false, isSelected = false,
                        )
                    ),
                    surchargeList = emptyList(), currencySymbol = "\u20b9", walletList = emptyList(), netBankingList = emptyList(),
                    amount = 499.0, buttonColor = "#000000", buttonTextColor = "#FFFFFF", ctaBorderRadius = 8,
                    onProceedForward = { _, _, _ -> },
                )
            }
        }

        // "More payment methods" only shows the "Cards" entry point when no
        // saved card exists yet -- once one does, it's shown elsewhere
        // (SavedCardComponent), not duplicated here.
        composeTestRule.onNodeWithText("Cards").assertDoesNotExist()
    }

    @Test
    fun `tapping the Cards row invokes onNavigateToCard`() {
        var navigated = false
        setContent(methodFlags = MainScreenModel.MethodFlags(isCardsVisible = true), onNavigateToCard = { navigated = true })

        composeTestRule.onNodeWithText("Cards").performClick()

        assertTrue(navigated)
    }

    @Test
    fun `no methods enabled renders nothing`() {
        setContent(methodFlags = MainScreenModel.MethodFlags())

        composeTestRule.onNodeWithText("Cards").assertDoesNotExist()
        composeTestRule.onNodeWithText("EMI").assertDoesNotExist()
        composeTestRule.onNodeWithText("Buy Now Pay Later").assertDoesNotExist()
    }
}
