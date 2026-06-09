package com.crossplatform.sdk.presentation.navigation

sealed class Routes(val route: String) {
    object MainScreen           : Routes("main_screen")
    object CardScreen           : Routes("card_screen")
    object EMIScreen            : Routes("emi_screen")
    object UpiTimerScreen       : Routes("upi_timer_screen")
    object AddressScreen        : Routes("address_screen")
    object BNPLScreen           : Routes("bnpl_screen")
    object NetBankingScreen     : Routes("net_banking_screen")
    object WalletScreen         : Routes("wallet_screen")
    object SavedAddressScreen   : Routes("saved_address_screen")
    object InstantOfferScreen   : Routes("instant_offer_screen")
}