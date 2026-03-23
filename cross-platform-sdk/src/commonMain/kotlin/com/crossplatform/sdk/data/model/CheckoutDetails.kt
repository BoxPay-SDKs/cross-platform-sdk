package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutDetails(
    // ─── Payment Info ───────────────────────────
    val currencySymbol: String,
    val currencyCode: String,
    val amount: String,
    val token: String,
    val env: String,
    val itemsLength: Int,
    val errorMessage: String,
    val shopperToken: String? = null,          // string | null

    // ─── Theme ──────────────────────────────────
    val fontFamily: FontConfiguration? = null,
    val ctaBorderRadius: Double,
    val buttonColor: String,
    val buttonTextColor: String,
    val headerColor: String,
    val headerTextColor: String,

    // ─── Screen Visibility ───────────────────────
    val isSuccessScreenVisible: Boolean,
    val isOrderItemDetailsVisible: Boolean,
    val isSICheckboxVisible: Boolean,
    val isSubscriptionCheckout: Boolean,

    // ─── Shopper Fields ──────────────────────────
    val isShippingAddressEnabled: Boolean,
    val isShippingAddressEditable: Boolean,
    val isFullNameEnabled: Boolean,
    val isFullNameEditable: Boolean,
    val isEmailEnabled: Boolean,
    val isEmailEditable: Boolean,
    val isPhoneEnabled: Boolean,
    val isPhoneEditable: Boolean,
    val isPanEnabled: Boolean,
    val isPanEditable: Boolean,
    val isDOBEnabled: Boolean,
    val isDOBEditable: Boolean,

    // ─── Payment Methods ─────────────────────────
    val isUpiIntentMethodEnabled: Boolean,
    val isUpiCollectMethodEnabled: Boolean,
    val isUpiQRMethodEnabled: Boolean,
    val isCardMethodEnabled: Boolean,
    val isWalletMethodEnabled: Boolean,
    val isNetBankingMethodEnabled: Boolean,
    val isEmiMethodEnabled: Boolean,
    val isBnplMethodEnabled: Boolean,
    val isUPIOtmIntentMethodEnabled: Boolean,
    val isUPIOtmCollectMethodEnabled: Boolean,
    val isUPIOtmQRMethodEnabled: Boolean,

    // ─── SDK Version ─────────────────────────
    val sdkVersion : String
)

@Serializable
data class FontConfiguration(
    @SerialName("regular") val regular: String? = null,       // regular?: string
    @SerialName("medium") val medium: String? = null,         // medium?: string
    @SerialName("semiBold") val semiBold: String? = null,     // semiBold?: string
    @SerialName("bold") val bold: String? = null,             // bold?: string
    @SerialName("extraBold") val extraBold: String? = null    // extraBold?: string
)