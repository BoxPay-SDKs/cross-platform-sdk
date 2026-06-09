package com.crossplatform.sdk.data.model

import com.crossplatform.sdk.domain.model.SurchargeModel
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutDetails(
    // ─── Payment Info ───────────────────────────
    val currencySymbol: String,
    val currencyCode: String,
    val amountBeforeSurcharge : Double,
    val discountAmount : Double,
    val amount: Double,
    val token: String,
    val isTestEnv: Boolean,
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
    val isFailedScreenVisible : Boolean,
    val isOrderItemDetailsVisible: Boolean,
    val isSICheckboxChecked: Boolean,
    val isSICheckboxEnabled : Boolean,
    val isSubscriptionCheckout: Boolean,
    val showQROnLoad : Boolean,
    val focusedTextInputBorderColor : String,
    val unfocusedTextInputBorderColor : String,

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
    val isMerchantLogoVisible : Boolean,
    val isSessionExpiryVisible : Boolean,

    // ---------Payment Current Status --------
    val status : String,
    val transactionId : String,
    val inquiryToken : String,
    val surchargeDetails : List<SurchargeModel>,
    val isWebViewVisible : Boolean,
    val appliedOfferId : String?,
    val subscription : List<Pair<String, String>>?,

    // -- BoxPay Elements
    val paymentMethodList : List<String>,
    val isBoxPayButtonVisible : Boolean
)