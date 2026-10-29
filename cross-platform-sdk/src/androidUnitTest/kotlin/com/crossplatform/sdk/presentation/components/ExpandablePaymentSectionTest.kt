package com.crossplatform.sdk.presentation.components

import com.crossplatform.sdk.fakes.initializeKamelApplicationContext

import androidx.test.core.app.ApplicationProvider

import androidx.compose.ui.platform.LocalInspectionMode

import androidx.compose.runtime.CompositionLocalProvider

import org.jetbrains.compose.resources.PreviewContextConfigurationEffect

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.domain.model.SurchargeModel
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_netbanking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/*
 * `ExpandablePaymentSection` now hoists its expand/collapse state to the
 * caller (`isExpanded` / `setIsExpanded` / `rotate`) rather than managing it
 * internally, and takes `surchargeList: List<SurchargeModel>` (applied
 * per-provider inside the nested `PaymentSelector`) instead of a single
 * `surchargeFee: Double?` rendered in the header. These tests drive the
 * hoisted state exactly as a real caller (e.g. `MorePaymentMethods`) would.
 */
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
        surchargeList: List<SurchargeModel> = emptyList(),
        onViewMore: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) {
                var isExpanded by remember { mutableStateOf(false) }
                ExpandablePaymentSection(
                    title = "All Banks",
                    image = Res.drawable.ic_netbanking,
                    providerList = providerList,
                    surchargeList = surchargeList,
                    currencySymbol = "₹",
                    amount = 499.0,
                    selectedId = "",
                    buttonTextColor = "#FFFFFF",
                    buttonColor = "#000000",
                    ctaBorderRadius = 8,
                    onClickRadio = { _, _ -> },
                    onProceedForward = { _, _, _ -> },
                    onViewMore = onViewMore,
                    rotate = if (isExpanded) 180f else 0f,
                    isExpanded = isExpanded,
                    setIsExpanded = { isExpanded = !isExpanded },
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
