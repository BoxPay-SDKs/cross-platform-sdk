package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_card
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MorePaymentContainerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the title and no surcharge line when surchargeFee is null`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                MorePaymentContainer(title = "Cards", image = Res.drawable.ic_card, surchargeFee = null, onClick = {}, currencySymbol = "\u20b9")
            }
        }

        composeTestRule.onNodeWithText("Cards").assertExists()
        composeTestRule.onNodeWithText("extra applied as surcharge", substring = true).assertDoesNotExist()
    }

    @Test
    fun `shows the surcharge line when surchargeFee is non-zero`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                MorePaymentContainer(title = "Cards", image = Res.drawable.ic_card, surchargeFee = 15.0, onClick = {}, currencySymbol = "\u20b9")
            }
        }

        composeTestRule.onNodeWithText("\u20b9 15.0 extra applied as surcharge").assertExists()
    }

    @Test
    fun `zero surchargeFee is treated the same as null - no surcharge line`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                MorePaymentContainer(title = "Cards", image = Res.drawable.ic_card, surchargeFee = 0.0, onClick = {}, currencySymbol = "\u20b9")
            }
        }

        composeTestRule.onNodeWithText("extra applied as surcharge", substring = true).assertDoesNotExist()
    }

    @Test
    fun `tapping the row invokes onClick`() {
        var clicked = false
        composeTestRule.setContent {
            ProvideSDKFonts {
                MorePaymentContainer(title = "EMI", image = Res.drawable.ic_card, onClick = { clicked = true }, currencySymbol = "\u20b9")
            }
        }

        composeTestRule.onNodeWithText("EMI").performClick()

        assertTrue(clicked)
    }
}
