package com.crossplatform.sdk.data.handler

import com.crossplatform.sdk.domain.mapper.toUiModel
import com.crossplatform.sdk.presentation.viewmodel.buildMinimalSessionDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Text-field border colors:
 *   - a color passed by the merchant when launching checkout wins;
 *   - an empty value ("None") falls back to the color from the checkout settings (session theme).
 *
 * The test session theme (see buildMinimalCheckoutTheme) uses #CCCCCC focused / #DDDDDD unfocused.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutBorderColorFallbackTest {

    private val themeFocused = "#CCCCCC"
    private val themeUnfocused = "#DDDDDD"

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        CommonSDKDismissHandler.releaseSession()
    }

    @AfterTest
    fun tearDown() {
        CommonSDKDismissHandler.releaseSession()
        Dispatchers.resetMain()
    }

    private fun launch(focused: String, unfocused: String, token: String = "session_token") =
        CheckoutDetailsHandler.setCheckoutToken(
            shopperToken                  = null,
            token                         = token,
            isTestEnv                     = true,
            isSuccessScreenVisible        = false,
            ctaBorderRadius               = 8,
            isSICheckboxChecked           = false,
            isSICheckboxEnabled           = false,
            isFailedScreenVisible         = false,
            showQROnLoad                  = false,
            focusedTextInputBorderColor   = focused,
            unfocusedTextInputBorderColor = unfocused
        )

    /** Same as the app: session response arrives and is mapped, which applies the theme. */
    private fun loadSession() {
        buildMinimalSessionDetails().toUiModel()
    }

    private fun assertColors(focused: String, unfocused: String) {
        assertEquals(focused, CheckoutDetailsHandler.focusedBorderColorFlow.value)
        assertEquals(unfocused, CheckoutDetailsHandler.unfocusedBorderColorFlow.value)
    }

    @Test
    fun `None uses the checkout settings colors`() {
        launch(focused = "", unfocused = "")
        loadSession()

        assertColors(themeFocused, themeUnfocused)
    }

    @Test
    fun `colors passed by the merchant win over the checkout settings`() {
        launch(focused = "#111111", unfocused = "#222222")
        loadSession()

        assertColors("#111111", "#222222")
    }

    @Test
    fun `only the focused color passed - unfocused falls back to checkout settings`() {
        launch(focused = "#111111", unfocused = "")
        loadSession()

        assertColors("#111111", themeUnfocused)
    }

    @Test
    fun `only the unfocused color passed - focused falls back to checkout settings`() {
        launch(focused = "", unfocused = "#222222")
        loadSession()

        assertColors(themeFocused, "#222222")
    }

    @Test
    fun `None after a custom-color launch uses checkout settings once the first checkout is released`() {
        // Launch 1: custom colors
        launch(focused = "#111111", unfocused = "#222222")
        loadSession()
        assertColors("#111111", "#222222")

        // Checkout closes through any exit path
        CommonSDKDismissHandler.releaseSession()

        // Launch 2: None
        launch(focused = "", unfocused = "")
        loadSession()

        assertColors(themeFocused, themeUnfocused)
    }

    @Test
    fun `a different custom color on the next launch replaces the previous one`() {
        launch(focused = "#111111", unfocused = "#222222")
        loadSession()
        CommonSDKDismissHandler.releaseSession()

        launch(focused = "#333333", unfocused = "#444444")
        loadSession()

        assertColors("#333333", "#444444")
    }

    @Test
    fun `new custom color shows immediately after launch without waiting for another launch`() {
        launch(focused = "#111111", unfocused = "#222222")
        CommonSDKDismissHandler.releaseSession()

        launch(focused = "#333333", unfocused = "#444444")

        // Before the session response arrives
        assertColors("#333333", "#444444")
    }

    @Test
    fun `repeated launch calls with None keep the checkout settings colors`() {
        // setCheckoutToken runs on every recomposition of the checkout root.
        launch(focused = "", unfocused = "")
        loadSession()

        launch(focused = "", unfocused = "")
        launch(focused = "", unfocused = "")

        assertColors(themeFocused, themeUnfocused)
    }

    @Test
    fun `repeated launch calls with a custom color keep the custom color after session load`() {
        launch(focused = "#111111", unfocused = "#222222")
        loadSession()

        launch(focused = "#111111", unfocused = "#222222")
        loadSession()

        assertColors("#111111", "#222222")
    }

    @Test
    fun `releaseSession then None before session load does not show the previous custom color`() {
        launch(focused = "#111111", unfocused = "#222222")
        loadSession()
        CommonSDKDismissHandler.releaseSession()

        launch(focused = "", unfocused = "")

        assertEquals("", CheckoutDetailsHandler.checkoutDetails.focusedTextInputBorderColor)
        assertEquals("", CheckoutDetailsHandler.checkoutDetails.unfocusedTextInputBorderColor)
    }
}
