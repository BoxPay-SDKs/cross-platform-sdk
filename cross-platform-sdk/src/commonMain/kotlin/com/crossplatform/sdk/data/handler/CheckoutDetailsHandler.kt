package com.crossplatform.sdk.data.handler

import com.crossplatform.sdk.data.model.CheckoutDetails
import com.crossplatform.sdk.data.model.FontConfiguration


object CheckoutDetailsHandler {

    // ─── Default Values (equivalent of your default object) ───
    private fun defaultCheckoutDetails() = CheckoutDetails(
        currencySymbol = "",
        currencyCode = "",
        amount = "",
        token = "",
        fontFamily = FontConfiguration(),
        ctaBorderRadius = 0.0,
        buttonColor = "#1CA672",
        buttonTextColor = "white",
        headerColor = "white",
        headerTextColor = "#363840",
        env = "",
        itemsLength = 0,
        errorMessage = "",
        shopperToken = null,
        isSuccessScreenVisible = false,
        isShippingAddressEnabled = false,
        isShippingAddressEditable = false,
        isFullNameEnabled = false,
        isFullNameEditable = false,
        isEmailEnabled = false,
        isEmailEditable = false,
        isPhoneEnabled = false,
        isPhoneEditable = false,
        isPanEnabled = false,
        isPanEditable = false,
        isDOBEnabled = false,
        isDOBEditable = false,
        isUpiIntentMethodEnabled = false,
        isUpiCollectMethodEnabled = false,
        isUpiQRMethodEnabled = false,
        isCardMethodEnabled = false,
        isWalletMethodEnabled = false,
        isNetBankingMethodEnabled = false,
        isEmiMethodEnabled = false,
        isBnplMethodEnabled = false,
        isUPIOtmIntentMethodEnabled = false,
        isUPIOtmCollectMethodEnabled = false,
        isUPIOtmQRMethodEnabled = false,
        isOrderItemDetailsVisible = true,
        isSICheckboxVisible = false,
        isSubscriptionCheckout = false,
        sdkVersion = "1.0.0"
    )

    // ─── State ─────────────────────────────────────────────────
    var checkoutDetails: CheckoutDetails = defaultCheckoutDetails()
        private set  // only settable inside this object

    // ─── setCheckoutDetailsHandler ─────────────────────────────
    fun set(details: CheckoutDetails) {
        checkoutDetails = details
    }

    // ─── setCheckOutDetailsHandlerToDefault ────────────────────
    fun resetToDefault() {
        checkoutDetails = defaultCheckoutDetails()
    }

    // ─── Update single fields using copy() ────────────────────
    fun updateToken(token: String) {
        checkoutDetails = checkoutDetails.copy(token = token)
    }

    fun updateShopperToken(shopperToken: String?) {
        checkoutDetails = checkoutDetails.copy(shopperToken = shopperToken)
    }
}