package com.crossplatform.sdk.presentation.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [Routes] is the single source of truth for every navigation destination
 * string in [AppNavHost]. This test locks down the literal route values —
 * a typo here would silently break deep linking or back-stack matching
 * (e.g. `AppNavHost`'s `baseRoute = currentRoute?.substringBefore("/{")`
 * comparison against `Routes.CardScreen.route`).
 */
class RoutesTest {

    @Test
    fun `every route has a distinct, non-blank value`() {
        val routes = listOf(
            Routes.MainScreen, Routes.CardScreen, Routes.EMIScreen, Routes.UpiTimerScreen,
            Routes.AddressScreen, Routes.BNPLScreen, Routes.NetBankingScreen, Routes.WalletScreen,
            Routes.SavedAddressScreen, Routes.InstantOfferScreen,
        )

        assertTrue(routes.all { it.route.isNotBlank() })
        assertEquals(routes.size, routes.map { it.route }.toSet().size) // no duplicates
    }

    @Test
    fun `route values match what AppNavHost builds parameterized paths from`() {
        // AppNavHost builds routes like "${Routes.CardScreen.route}/$isAutoNavigationEnabled".
        // If these literal values ever change, every deep-link/back-stack
        // comparison in AppNavHost silently breaks, so pin them explicitly.
        assertEquals("main_screen", Routes.MainScreen.route)
        assertEquals("card_screen", Routes.CardScreen.route)
        assertEquals("emi_screen", Routes.EMIScreen.route)
        assertEquals("upi_timer_screen", Routes.UpiTimerScreen.route)
        assertEquals("address_screen", Routes.AddressScreen.route)
        assertEquals("bnpl_screen", Routes.BNPLScreen.route)
        assertEquals("net_banking_screen", Routes.NetBankingScreen.route)
        assertEquals("wallet_screen", Routes.WalletScreen.route)
        assertEquals("saved_address_screen", Routes.SavedAddressScreen.route)
        assertEquals("instant_offer_screen", Routes.InstantOfferScreen.route)
    }
}
