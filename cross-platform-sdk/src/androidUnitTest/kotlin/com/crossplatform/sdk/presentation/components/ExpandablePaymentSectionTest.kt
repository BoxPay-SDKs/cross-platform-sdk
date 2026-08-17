package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class ExpandablePaymentSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun provider(id: String, name: String) = SelectedPaymentMethod(
        type = "NetBanking", id = id, displayName = name, displayValue = name,
        imageUrl = "", instrumentType = "netbanking", isLastUsed = false, isSelected = false,
    )

    private fun setContent(
        providerList: List<SelectedPaymentMethod>,
        surchargeFee: Double? = null,
        onViewMore: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ProvideSDKFonts {
                ExpandablePaymentSection(
                    title = "All Banks",
                    image = Res.drawable.ic_netbanking,
                    providerList = providerList,
                    surchargeFee = surchargeFee,
                    currencySymbol = "\u20b9",
                    amount = 499.0,
                    selectedId = "",
                    buttonTextColor = "#FFFFFF",
                    buttonColor = "#000000",
                    ctaBorderRadius = 8,
                    onClickRadio = {},
                    onProceedForward = { _, _, _ -> },
                    onViewMore = onViewMore,
                )
            }
        }
    }

    @Test
    fun `providers are hidden until the header is tapped`() {
        setContent(providerList = listOf(provider("hdfc", "HDFC Bank")))

        composeTestRule.onNodeWithText("All Banks").assertExists()
        composeTestRule.onNodeWithText("HDFC Bank").assertDoesNotExist()
    }

    @Test
    fun `tapping the header expands and reveals the provider list`() {
        setContent(providerList = listOf(provider("hdfc", "HDFC Bank")))

        composeTestRule.onNodeWithText("All Banks").performClick()

        composeTestRule.onNodeWithText("HDFC Bank").assertExists()
    }

    @Test
    fun `tapping the header again collapses it`() {
        setContent(providerList = listOf(provider("hdfc", "HDFC Bank")))
        composeTestRule.onNodeWithText("All Banks").performClick() // expand

        composeTestRule.onNodeWithText("All Banks").performClick() // collapse

        composeTestRule.onNodeWithText("HDFC Bank").assertDoesNotExist()
    }

    @Test
    fun `a non-zero surcharge fee is shown in the header`() {
        setContent(providerList = emptyList(), surchargeFee = 15.0)

        composeTestRule.onNodeWithText("\u20b9 15.0 extra applied as surcharge").assertExists()
    }

    @Test
    fun `a zero surcharge fee is not shown`() {
        setContent(providerList = emptyList(), surchargeFee = 0.0)

        composeTestRule.onNodeWithText("extra applied as surcharge", substring = true).assertDoesNotExist()
    }

    @Test
    fun `View More appears only when expanded with more than 4 providers, and invokes the callback`() {
        var viewMoreClicked = false
        setContent(
            providerList = (1..5).map { provider(id = "bank_$it", name = "Bank $it") },
            onViewMore = { viewMoreClicked = true },
        )
        composeTestRule.onNodeWithText("View More").assertDoesNotExist() // collapsed, not shown yet

        composeTestRule.onNodeWithText("All Banks").performClick() // expand
        composeTestRule.onNodeWithText("View More").assertExists()

        composeTestRule.onNodeWithText("View More").performClick()
        kotlin.test.assertTrue(viewMoreClicked)
    }

    @Test
    fun `View More does not appear with 4 or fewer providers`() {
        setContent(providerList = (1..4).map { provider(id = "bank_$it", name = "Bank $it") })

        composeTestRule.onNodeWithText("All Banks").performClick() // expand

        composeTestRule.onNodeWithText("View More").assertDoesNotExist()
    }
}
