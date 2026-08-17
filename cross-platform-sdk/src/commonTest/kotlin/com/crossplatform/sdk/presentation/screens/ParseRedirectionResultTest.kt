package com.crossplatform.sdk.presentation.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [parseRedirectionResult] is the pure logic `WebViewScreen`'s platform
 * `actual` implementations call on every navigation to decide whether the
 * payment flow has completed. It's the only piece of that file that isn't
 * an `expect`/`actual` platform composable, so it's covered directly here
 * rather than through a Compose UI test — see TEST_PLAN.md for why full
 * `WebViewScreen` rendering isn't attempted.
 */
class ParseRedirectionResultTest {

    @Test
    fun `returns the redirectionResult value when both markers are present`() {
        val url = "https://pay.example.com/payment-completion-handler/boxpay?foo=bar&redirectionResult=SUCCESS123"

        assertEquals("SUCCESS123", parseRedirectionResult(url))
    }

    @Test
    fun `returns null when neither marker is present`() {
        val url = "https://pay.example.com/some-other-page?redirectionResult=SUCCESS123"

        assertNull(parseRedirectionResult(url))
    }

    @Test
    fun `returns null when the completion marker is present but redirectionResult param is missing`() {
        val url = "https://pay.example.com/payment-completion-handler/boxpay?foo=bar"

        assertNull(parseRedirectionResult(url))
    }

    @Test
    fun `returns null when there is no query string at all`() {
        val url = "https://pay.example.com/payment-completion-handler/boxpay"

        assertNull(parseRedirectionResult(url))
    }

    @Test
    fun `returns null when redirectionResult is present but empty`() {
        val url = "https://pay.example.com/payment-completion-handler/boxpay?redirectionResult="

        assertNull(parseRedirectionResult(url))
    }

    @Test
    fun `finds redirectionResult regardless of its position among other params`() {
        val url = "https://pay.example.com/boxpay?a=1&redirectionResult=ABC&b=2"

        assertEquals("ABC", parseRedirectionResult(url))
    }

    @Test
    fun `only one of the two markers needs to be present`() {
        assertEquals("X", parseRedirectionResult("https://x.com/boxpay?redirectionResult=X"))
        assertEquals("Y", parseRedirectionResult("https://x.com/payment-completion-handler?redirectionResult=Y"))
    }
}
