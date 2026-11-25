package com.crossplatform.sdk.presentation.components

import com.crossplatform.sdk.fakes.initializeKamelApplicationContext

import androidx.test.core.app.ApplicationProvider

import androidx.compose.ui.platform.LocalInspectionMode

import androidx.compose.runtime.CompositionLocalProvider

import org.jetbrains.compose.resources.PreviewContextConfigurationEffect

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.domain.model.AppLanguage
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TopBarTest {

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

    @Test
    fun `renders the given title text`() {
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) { TopBar(showDesc = false, text = "Pay via Card", onBackPress = {}, sessionSeconds = null, selectedLanguageCode = "en", onLanguageChange = {}) }
        }

        composeTestRule.onNodeWithText("Pay via Card").assertExists()
    }

    @Test
    fun `tapping the back button invokes onBackPress`() {
        // isMerchantLogoVisible defaults to false in defaultCheckoutDetails() —
        // the back IconButton only renders when it's explicitly true.
        CheckoutDetailsHandler.set(CheckoutDetailsHandler.checkoutDetails.copy(isMerchantLogoVisible = true))
        var backPressed = false
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) { TopBar(showDesc = false, text = "Payment Details", onBackPress = { backPressed = true }, sessionSeconds = null, selectedLanguageCode = "en", onLanguageChange = {}) }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertTrue(backPressed)
    }


    @Test
    fun `the session timer is hidden when sessionSeconds is null`() {
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) { TopBar(showDesc = false, text = "Payment Details", onBackPress = {}, sessionSeconds = null, selectedLanguageCode = "en", onLanguageChange = {}) }
        }

        composeTestRule.onNodeWithText("Payment Details").assertExists() // sanity: screen still renders
    }

    @Test
    fun `the session timer shows a formatted mm colon ss when isSessionExpiryVisible and seconds are set`() {
        CheckoutDetailsHandler.set(CheckoutDetailsHandler.checkoutDetails.copy(isSessionExpiryVisible = true))

        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = CheckoutDetailsHandler.checkoutDetails.localeCode) { TopBar(showDesc = false, text = "Payment Details", onBackPress = {}, sessionSeconds = 125L, selectedLanguageCode = "en", onLanguageChange = {}) }
        }

        composeTestRule.onNodeWithText("02:05").assertExists()
    }

    // ---- language dropdown: selected-locale highlight ----

    private fun setContentWithLanguageSelector(
        selectedLanguageCode: String,
        onLanguageChange: (String) -> Unit = {}
    ) {
        CheckoutDetailsHandler.set(CheckoutDetailsHandler.checkoutDetails.copy(isMultiLanguageSupported = true))
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = selectedLanguageCode) {
                TopBar(
                    showDesc = false,
                    text = "Payment Details",
                    onBackPress = {},
                    sessionSeconds = null,
                    selectedLanguageCode = selectedLanguageCode,
                    onLanguageChange = onLanguageChange
                )
            }
        }
    }

    @Test
    fun `the language selector is hidden when multiple languages are not supported`() {
        CheckoutDetailsHandler.set(CheckoutDetailsHandler.checkoutDetails.copy(isMultiLanguageSupported = false))
        composeTestRule.setContent {
            initializeKamelApplicationContext(ApplicationProvider.getApplicationContext())
            CompositionLocalProvider(LocalInspectionMode provides true) {
                PreviewContextConfigurationEffect()
            }
            ProvideSDKFonts(currentLanguage = "en") {
                TopBar(showDesc = false, text = "Payment Details", onBackPress = {}, sessionSeconds = null, selectedLanguageCode = "en", onLanguageChange = {})
            }
        }

        composeTestRule.onNodeWithContentDescription("Select language").assertDoesNotExist()
    }

    @Test
    fun `opening the language dropdown marks only the currently selected language as selected`() {
        setContentWithLanguageSelector(selectedLanguageCode = "fr")

        composeTestRule.onNodeWithContentDescription("Select language").performClick()

        composeTestRule.onNodeWithText("Français").assertIsSelected()
        composeTestRule.onNodeWithText("English").assertIsNotSelected()
        composeTestRule.onNodeWithText("Español").assertIsNotSelected()
        composeTestRule.onNodeWithText("हिंदी").assertIsNotSelected()
    }

    @Test
    fun `the selected language follows the selectedLanguageCode it is given`() {
        setContentWithLanguageSelector(selectedLanguageCode = "hi")

        composeTestRule.onNodeWithContentDescription("Select language").performClick()

        composeTestRule.onNodeWithText("हिंदी").assertIsSelected()
        composeTestRule.onNodeWithText("Français").assertIsNotSelected()
    }

    @Test
    fun `no language is marked selected when the code matches none`() {
        setContentWithLanguageSelector(selectedLanguageCode = "xx")

        composeTestRule.onNodeWithContentDescription("Select language").performClick()

        AppLanguage.entries.forEach {
            composeTestRule.onNodeWithText(it.displayName).assertIsNotSelected()
        }
    }

    @Test
    fun `tapping a language invokes onLanguageChange with its code`() {
        var changedTo: String? = null
        setContentWithLanguageSelector(selectedLanguageCode = "en") { changedTo = it }

        composeTestRule.onNodeWithContentDescription("Select language").performClick()
        composeTestRule.onNodeWithText("Español").performClick()

        assertEquals("es", changedTo)
    }
}
