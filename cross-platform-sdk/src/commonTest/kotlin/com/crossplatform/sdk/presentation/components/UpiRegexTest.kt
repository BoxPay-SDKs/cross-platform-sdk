package com.crossplatform.sdk.presentation.components

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [upiRegex] validates a UPI VPA (e.g. "jane@okhdfcbank") before the "Pay"
 * button in [UPIComponent] is enabled. Covered as a plain unit test since it
 * needs no Compose rendering — full rendering of [UPIComponent] itself is
 * riskier under Robolectric due to its platform-specific
 * `getInstalledUpiApps()`/`getPlatformContext()` calls (see TEST_PLAN.md).
 */
class UpiRegexTest {

    @Test
    fun `accepts a typical VPA`() {
        assertTrue(upiRegex.matches("jane@okhdfcbank"))
    }

    @Test
    fun `accepts a VPA with digits and dots in the handle`() {
        assertTrue(upiRegex.matches("jane.doe.99@upi"))
    }

    @Test
    fun `rejects a VPA missing the @ separator`() {
        assertFalse(upiRegex.matches("janeokhdfcbank"))
    }

    @Test
    fun `rejects a VPA whose provider is fewer than 3 characters`() {
        assertFalse(upiRegex.matches("jane@ok"))
    }

    @Test
    fun `rejects a VPA whose handle is only 1 character`() {
        assertFalse(upiRegex.matches("j@okhdfcbank"))
    }

    @Test
    fun `rejects a provider containing digits`() {
        assertFalse(upiRegex.matches("jane@okhdfc2"))
    }

    @Test
    fun `rejects a blank string`() {
        assertFalse(upiRegex.matches(""))
    }
}
