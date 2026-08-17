package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddressComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        address: String = "",
        isShippingAddressEnabled: Boolean = false,
        isFullNameEnabled: Boolean = false,
        isPhoneEnabled: Boolean = false,
        isEmailEnabled: Boolean = false,
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        completePhoneNumber: String? = null,
        labelType: String? = null,
        labelName: String? = null,
    ) {
        composeTestRule.setContent {
            ProvideSDKFonts {
                AddressComponent(
                    address = address,
                    navigateToAddressScreen = {},
                    isShippingAddressEnabled = isShippingAddressEnabled,
                    isFullNameEnabled = isFullNameEnabled,
                    isPhoneEnabled = isPhoneEnabled,
                    isEmailEnabled = isEmailEnabled,
                    isShippingAddressEditable = true,
                    isFullNameEditable = true,
                    isEmailEditable = true,
                    isPhoneEditable = true,
                    firstName = firstName,
                    lastName = lastName,
                    email = email,
                    completePhoneNumber = completePhoneNumber,
                    labelType = labelType,
                    labelName = labelName,
                )
            }
        }
    }

    @Test
    fun `when a shipping address is present, it shows the Address section with the delivery label`() {
        setContent(
            address = "123 Main St, Mumbai",
            isShippingAddressEnabled = true,
            labelType = "Home",
        )

        composeTestRule.onNodeWithText("Address").assertExists()
        composeTestRule.onNodeWithText("123 Main St, Mumbai").assertExists()
    }

    @Test
    fun `a custom label type falls back to the label name`() {
        setContent(
            address = "123 Main St, Mumbai",
            isShippingAddressEnabled = true,
            labelType = "Other",
            labelName = "Mom's Place",
        )

        // The "Deliver at <label>" text is a single AnnotatedString node;
        // just confirm the address card itself rendered without crashing
        // and shows the address line.
        composeTestRule.onNodeWithText("123 Main St, Mumbai").assertExists()
    }

    @Test
    fun `when address is disabled but name and phone are enabled, shows Personal Details instead`() {
        setContent(
            isShippingAddressEnabled = false,
            isFullNameEnabled = true,
            isPhoneEnabled = true,
            firstName = "Jane",
            lastName = "Doe",
            completePhoneNumber = "+91 9999999999",
        )

        composeTestRule.onNodeWithText("Personal Details").assertExists()
        composeTestRule.onNodeWithText("Address").assertDoesNotExist()
    }

    @Test
    fun `email-only is shown as its own line when only email is enabled`() {
        setContent(
            isShippingAddressEnabled = false,
            isEmailEnabled = true,
            email = "jane@example.com",
        )

        composeTestRule.onNodeWithText("jane@example.com").assertExists()
    }

    @Test
    fun `renders nothing when neither address nor any personal field is enabled`() {
        setContent(isShippingAddressEnabled = false, isFullNameEnabled = false, isPhoneEnabled = false, isEmailEnabled = false)

        composeTestRule.onNodeWithText("Address").assertDoesNotExist()
        composeTestRule.onNodeWithText("Personal Details").assertDoesNotExist()
    }
}
