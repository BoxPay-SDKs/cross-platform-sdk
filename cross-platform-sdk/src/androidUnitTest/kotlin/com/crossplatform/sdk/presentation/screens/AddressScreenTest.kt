package com.crossplatform.sdk.presentation.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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

/**
 * `AddressScreen` doesn't use a ViewModel at all — it reads/writes
 * `UserDataHandler`/`CheckoutDetailsHandler` singletons directly — so no
 * Koin setup is needed, only `Dispatchers.setMain` for those singletons'
 * internal `Dispatchers.Main.immediate` scopes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddressScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun setContent(
        isShippingEnabled: Boolean,
        isFullNameEnabled: Boolean = false,
        isEmailEnabled: Boolean = false,
        isPhoneEnabled: Boolean = false,
    ) {
        composeTestRule.setContent {
            ProvideSDKFonts {
                AddressScreen(
                    onAddressSaved = {},
                    onBackPress = {},
                    isShippingEnabled = isShippingEnabled,
                    isFullNameEnabled = isFullNameEnabled,
                    isEmailEnabled = isEmailEnabled,
                    isPhoneEnabled = isPhoneEnabled,
                )
            }
        }
    }

    @Test
    fun `when shipping address is enabled, the Country field is shown and the button says Save Address`() {
        setContent(isShippingEnabled = true)

        composeTestRule.onNodeWithText("Country*").assertExists()
        composeTestRule.onNodeWithText("Save Address", substring = true).assertExists()
    }

    @Test
    fun `when shipping is disabled, the Country field is hidden and the button says Save Personal Details`() {
        setContent(isShippingEnabled = false, isFullNameEnabled = true)

        composeTestRule.onNodeWithText("Country*").assertDoesNotExist()
        composeTestRule.onNodeWithText("Save Personal Details", substring = true).assertExists()
    }

    @Test
    fun `full name field only shows when shipping or full name is enabled`() {
        setContent(isShippingEnabled = false, isFullNameEnabled = false)

        composeTestRule.onNodeWithText("Full Name*").assertDoesNotExist()
    }

    @Test
    fun `email field respects isEmailEnabled independently of the other fields`() {
        setContent(isShippingEnabled = false, isFullNameEnabled = false, isEmailEnabled = true, isPhoneEnabled = false)

        composeTestRule.onNodeWithText("Email ID*").assertExists()
        composeTestRule.onNodeWithText("Full Name*").assertDoesNotExist()
    }

    @Test
    fun `with every optional field disabled, only the save button remains`() {
        setContent(isShippingEnabled = false, isFullNameEnabled = false, isEmailEnabled = false, isPhoneEnabled = false)

        composeTestRule.onNodeWithText("Country*").assertDoesNotExist()
        composeTestRule.onNodeWithText("Full Name*").assertDoesNotExist()
        composeTestRule.onNodeWithText("Email ID*").assertDoesNotExist()
        composeTestRule.onNodeWithText("Save Personal Details", substring = true).assertExists()
    }
}
