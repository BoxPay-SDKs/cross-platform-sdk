package com.crossplatform.sdk.presentation.components

import com.crossplatform.sdk.fakes.initializeKamelApplicationContext

import androidx.test.core.app.ApplicationProvider

import androidx.compose.ui.platform.LocalInspectionMode

import androidx.compose.runtime.CompositionLocalProvider

import org.jetbrains.compose.resources.PreviewContextConfigurationEffect

import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BankComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun bank(id: String, name: String) = SelectedPaymentMethod(
        type = "NetBanking", id = id, displayName = name, displayValue = name,
        imageUrl = "", instrumentType = "netbanking", isLastUsed = false, isSelected = false,
    )

    private fun setContent(list: List<SelectedPaymentMethod>, searchQuery: String = "") {
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
                BankComponent(
                    modifier = Modifier,
                    searchQuery = searchQuery,
                    onSetSearchQuery = {},
                    focusedTextInputBorderColor = "#CCCCCC",
                    unfocusedTextInputBorderColor = "#DDDDDD",
                    list = list,
                    onProceedForward = { _, _, _ -> },
                    onClickRadio = { _, _ -> },
                    selectedInstrumentId = "",
                    buttonTextColor = "#FFFFFF",
                    buttonColor = "#000000",
                    amount = 499.0,
                    currencySymbol = "\u20b9",
                    ctaBorderRadius = 8,
                    title = "Select Bank",
                    surchargeList = emptyList(),
                )
            }
        }
    }

    @Test
    fun `shows the empty state when the bank list is empty`() {
        setContent(list = emptyList())

        composeTestRule.onNodeWithText("Oops!! No results found").assertExists()
    }

    @Test
    fun `shows the section title and each bank name when the list is populated`() {
        setContent(list = listOf(bank("hdfc", "HDFC Bank"), bank("sbi", "SBI")))

        composeTestRule.onNodeWithText("Select Bank").assertExists()
        composeTestRule.onNodeWithText("HDFC Bank").assertExists()
        composeTestRule.onNodeWithText("SBI").assertExists()
        composeTestRule.onNodeWithText("Oops!! No results found").assertDoesNotExist()
    }

    @Test
    fun `typing into the search field is possible`() {
        setContent(list = listOf(bank("hdfc", "HDFC Bank")))

        // Verifies the search field actually accepts input without crashing;
        // the filtering itself lives in the ViewModel (already covered by
        // NetBankingViewModelTest's onSearch tests), not this component.
        composeTestRule.onNodeWithText("Search").performTextInput("hdfc")
    }
}
