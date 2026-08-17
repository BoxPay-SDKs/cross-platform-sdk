package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_home
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_more
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SavedAddressCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        isCurrentlySelected: Boolean,
        onClickSelectAddress: () -> Unit = {},
        onClickEditAddress: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ProvideSDKFonts {
                SavedAddressCard(
                    modifier = Modifier,
                    address1 = "123 Main St", address2 = null, city = "Mumbai", state = "MH", pinCode = "400001",
                    number = "9999999999", isCurrentlySelected = isCurrentlySelected, addressIcon = Res.drawable.ic_home,
                    label = "Home", onClickEditAddress = onClickEditAddress, onClickSelectAddress = onClickSelectAddress,
                    selectedCtaColor = "#000000", editAddressIcon = Res.drawable.ic_more,
                )
            }
        }
    }

    @Test
    fun `shows the label`() {
        setContent(isCurrentlySelected = false)

        composeTestRule.onNodeWithText("Home").assertExists()
    }

    @Test
    fun `shows the CURRENTLY SELECTED tag only when selected`() {
        setContent(isCurrentlySelected = true)

        composeTestRule.onNodeWithText("CURRENTLY SELECTED").assertExists()
    }

    @Test
    fun `hides the CURRENTLY SELECTED tag when not selected`() {
        setContent(isCurrentlySelected = false)

        composeTestRule.onNodeWithText("CURRENTLY SELECTED").assertDoesNotExist()
    }

    @Test
    fun `tapping the card invokes onClickSelectAddress`() {
        var selected = false
        setContent(isCurrentlySelected = false, onClickSelectAddress = { selected = true })

        composeTestRule.onNodeWithText("Home").performClick()

        assertTrue(selected)
    }
}
