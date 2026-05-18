package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutDetails(
    // ─── Payment Info ───────────────────────────
    val currencySymbol: String,
    val currencyCode: String,
    val amount: Double,
    val token: String,
    val env: String,
    val itemsLength: Int,
    val errorMessage: String,
    val isSessionExpired : Boolean,
    val isPaymentSuccessful : Boolean,
    val successfulTimeStamp : String,
    val selectedPaymentMethod : String,
    val isPaymentFailed : Boolean,
    val shopperToken: String? = null,          // string | null

    // ─── Theme ──────────────────────────────────
//    val fontFamily: FontConfiguration? = null,
    val ctaBorderRadius: Int,
    val buttonColor: String,
    val buttonTextColor: String,
    val headerColor: String,
    val headerTextColor: String,
    val merchantName : String,
    val merchantLogo : String,

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

    // ---------Payment Current Status --------
    val status : String,
    val transactionId : String,
    val inquiryToken : String
)

@Serializable
data class FontConfiguration(
    @SerialName("regular") val regular: String? = null,       // regular?: string
    @SerialName("medium") val medium: String? = null,         // medium?: string
    @SerialName("semiBold") val semiBold: String? = null,     // semiBold?: string
    @SerialName("bold") val bold: String? = null,             // bold?: string
    @SerialName("extraBold") val extraBold: String? = null    // extraBold?: string
)