package com.crossplatform.sdk.data.handler

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [CommonSDKDismissHandler.releaseSession] is the single cleanup every exit path calls
 * (in-SDK close, activity destroy, Elements view dispose, iOS controller dispose).
 * These tests pin down what it clears, that it is safe to call repeatedly, and that it
 * leaves the next launch able to start from a clean handler.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CommonSDKDismissHandlerTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        CommonSDKDismissHandler.releaseSession()
        CommonSDKDismissHandler.setCloseSDK { }
    }

    @AfterTest
    fun tearDown() {
        CommonSDKDismissHandler.releaseSession()
        CommonSDKDismissHandler.setCloseSDK { }
        Dispatchers.resetMain()
    }

    private fun launch(
        token: String = "token_1",
        focused: String = "",
        unfocused: String = ""
    ) = CheckoutDetailsHandler.setCheckoutToken(
        shopperToken                  = "shopper_1",
        token                         = token,
        isTestEnv                     = true,
        isSuccessScreenVisible        = true,
        ctaBorderRadius               = 8,
        isSICheckboxChecked           = true,
        isSICheckboxEnabled           = true,
        isFailedScreenVisible         = true,
        showQROnLoad                  = true,
        focusedTextInputBorderColor   = focused,
        unfocusedTextInputBorderColor = unfocused
    )

    // ── what releaseSession clears ───────────────────────────────────────────

    @Test
    fun `releaseSession clears checkout details`() {
        launch(focused = "#111111", unfocused = "#222222")
        CheckoutDetailsHandler.setAmount(499.0)

        CommonSDKDismissHandler.releaseSession()

        val details = CheckoutDetailsHandler.checkoutDetails
        assertEquals("", details.token)
        assertEquals(null, details.shopperToken)
        assertEquals("", details.focusedTextInputBorderColor)
        assertEquals("", details.unfocusedTextInputBorderColor)
        assertEquals(0.0, details.amount)
        assertFalse(details.isTestEnv)
    }

    @Test
    fun `releaseSession clears the published border color flows`() {
        launch(focused = "#111111", unfocused = "#222222")
        assertEquals("#111111", CheckoutDetailsHandler.focusedBorderColorFlow.value)

        CommonSDKDismissHandler.releaseSession()

        assertEquals("", CheckoutDetailsHandler.focusedBorderColorFlow.value)
        assertEquals("", CheckoutDetailsHandler.unfocusedBorderColorFlow.value)
    }

    @Test
    fun `releaseSession clears user data`() {
        UserDataHandler.setUniqueRef("shopper_unique_ref")

        CommonSDKDismissHandler.releaseSession()

        assertEquals("", UserDataHandler.userData.uniqueId)
    }

    @Test
    fun `releaseSession cancels registered jobs`() {
        val running = Job()
        val another = Job()
        SDKJobHandler.register(running)
        SDKJobHandler.register(another)

        CommonSDKDismissHandler.releaseSession()

        assertTrue(running.isCancelled)
        assertTrue(another.isCancelled)
    }

    // ── safety / idempotence ─────────────────────────────────────────────────

    @Test
    fun `releaseSession is safe to call repeatedly`() {
        launch()

        CommonSDKDismissHandler.releaseSession()
        CommonSDKDismissHandler.releaseSession()
        CommonSDKDismissHandler.releaseSession()

        assertEquals("", CheckoutDetailsHandler.checkoutDetails.token)
    }

    @Test
    fun `releaseSession with nothing to release does not throw`() {
        CommonSDKDismissHandler.releaseSession()
        CommonSDKDismissHandler.releaseSession()
    }

    @Test
    fun `a new launch after releaseSession starts from a clean handler`() {
        launch(token = "token_1", focused = "#111111", unfocused = "#222222")
        CommonSDKDismissHandler.releaseSession()

        launch(token = "token_2", focused = "", unfocused = "")

        val details = CheckoutDetailsHandler.checkoutDetails
        assertEquals("token_2", details.token)
        assertEquals("", details.focusedTextInputBorderColor)
        assertEquals("", details.unfocusedTextInputBorderColor)
    }

    // ── close callback ───────────────────────────────────────────────────────

    @Test
    fun `notifyToCloseSDK invokes the registered close callback`() {
        var closed = 0
        CommonSDKDismissHandler.setCloseSDK { closed++ }

        CommonSDKDismissHandler.notifyToCloseSDK()

        assertEquals(1, closed)
    }

    @Test
    fun `setCloseSDK replaces the previous callback`() {
        var first = 0
        var second = 0
        CommonSDKDismissHandler.setCloseSDK { first++ }
        CommonSDKDismissHandler.setCloseSDK { second++ }

        CommonSDKDismissHandler.notifyToCloseSDK()

        assertEquals(0, first)
        assertEquals(1, second)
    }

    @Test
    fun `releaseSession does not trigger the close callback`() {
        // onDestroy / dispose paths call releaseSession while the host is already closing;
        // calling close again from there would re-enter finish()/dismiss.
        var closed = 0
        CommonSDKDismissHandler.setCloseSDK { closed++ }

        CommonSDKDismissHandler.releaseSession()

        assertEquals(0, closed)
    }
}
