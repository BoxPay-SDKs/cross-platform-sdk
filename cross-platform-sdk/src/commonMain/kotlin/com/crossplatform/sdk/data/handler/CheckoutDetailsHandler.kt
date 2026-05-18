package com.crossplatform.sdk.data.handler

import com.crossplatform.sdk.data.model.CheckoutDetails
import com.crossplatform.sdk.data.model.FontConfiguration
import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


object CheckoutDetailsHandler {

    // ─── Default Values (equivalent of your default object) ───
    private fun defaultCheckoutDetails() = CheckoutDetails(
        currencySymbol = "",
        currencyCode = "",
        amount = 0.0,
        token = "",
        isSessionExpired = false,
        isPaymentFailed = false,
        successfulTimeStamp = "",
        selectedPaymentMethod = "",
        isPaymentSuccessful = false,
        merchantLogo = "",
        merchantName = "",
//        fontFamily = FontConfiguration(),
        ctaBorderRadius = 12,
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
        isOrderItemDetailsVisible = true,
        isSICheckboxVisible = false,
        isSubscriptionCheckout = false,
        status = TransactionStatusEnum.NOACTION.name,
        transactionId = "",
        inquiryToken = ""
    )

    // ─── State ─────────────────────────────────────────────────
    private val _checkoutDetailsFlow = MutableStateFlow(defaultCheckoutDetails())
    val checkoutDetailsFlow: StateFlow<CheckoutDetails> = _checkoutDetailsFlow
    var checkoutDetails: CheckoutDetails = defaultCheckoutDetails()
        private set  // only settable inside this object

    // ─── setCheckoutDetailsHandler ─────────────────────────────
    fun set(details: CheckoutDetails) {
        checkoutDetails = details
        _checkoutDetailsFlow.value = details
    }

    fun setStatusAndTransID(status : String, transactionId : String) {
        checkoutDetails =
            checkoutDetails.copy(status = status, transactionId = transactionId)
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setInquiryToken(inquiryToken : String) {
        checkoutDetails =
            checkoutDetails.copy(inquiryToken = inquiryToken)
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setSDKConfig(
        currencySymbol : String,
        currencyCode : String,
        amount :Double,
        buttonColor : String,
        buttonTextColor : String,
        headerColor : String,
        headerTextColor : String,
        merchantName : String,
        merchantLogo : String,
        itemsLength : Int,
        errorMessage : String,
        isShippingAddressEnabled : Boolean,
        isShippingAddressEditable : Boolean,
        isFullNameEnabled : Boolean,
        isFullNameEditable : Boolean,
        isEmailEnabled : Boolean,
        isEmailEditable : Boolean,
        isPhoneEnabled : Boolean,
        isPhoneEditable : Boolean,
        isPanEnabled : Boolean,
        isPanEditable : Boolean,
        isDOBEnabled : Boolean,
        isDOBEditable : Boolean,
        isOrderItemDetailsVisible : Boolean,
        isSubscriptionCheckout : Boolean,
    ) {
        checkoutDetails = checkoutDetails.copy(
            currencySymbol = currencySymbol,
            currencyCode = currencyCode,
            amount = amount,
            buttonColor = buttonColor,
            buttonTextColor = buttonTextColor,
            headerColor = headerColor,
            headerTextColor = headerTextColor,
            itemsLength = itemsLength,
            merchantName = merchantName,
            merchantLogo = merchantLogo,
            errorMessage = errorMessage,
            isShippingAddressEnabled = isShippingAddressEnabled,
            isShippingAddressEditable = isShippingAddressEditable,
            isFullNameEnabled = isFullNameEnabled,
            isFullNameEditable = isFullNameEditable,
            isEmailEnabled = isEmailEnabled,
            isEmailEditable = isEmailEditable,
            isPhoneEnabled = isPhoneEnabled,
            isPhoneEditable = isPhoneEditable,
            isPanEnabled = isPanEnabled,
            isPanEditable = isPanEditable,
            isDOBEnabled = isDOBEnabled,
            isDOBEditable = isDOBEditable,
            isOrderItemDetailsVisible = isOrderItemDetailsVisible,
            isSubscriptionCheckout = isSubscriptionCheckout
        )
        _checkoutDetailsFlow.value = checkoutDetails
    }

    // ─── setCheckOutDetailsHandlerToDefault ────────────────────
    fun resetToDefault() {
        checkoutDetails = defaultCheckoutDetails()
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setCheckoutToken(shopperToken: String?, token : String, env : String, isSuccessScreenVisible: Boolean, ctaBorderRadius: Int, isSICheckboxVisible : Boolean,) {
        checkoutDetails = checkoutDetails.copy(shopperToken = shopperToken, token = token, env = env, isSuccessScreenVisible = isSuccessScreenVisible, ctaBorderRadius = ctaBorderRadius, isSICheckboxVisible = isSICheckboxVisible)
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setSessionExpired() {
        checkoutDetails = checkoutDetails.copy(isSessionExpired = !checkoutDetails.isPaymentFailed)
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setSessionFailed() {
        checkoutDetails = checkoutDetails.copy(isPaymentFailed = !checkoutDetails.isPaymentFailed)
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setSessionSuccess() {
        checkoutDetails = checkoutDetails.copy(isPaymentSuccessful = !checkoutDetails.isPaymentSuccessful)
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setErrorMessage(message : String = "You may have cancelled the payment or there was a delay in response. Please retry.") {
        checkoutDetails = checkoutDetails.copy(errorMessage = message)
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setTimeAndPaymentMethod(timeStamp : String, paymentMethod : String) {
        checkoutDetails = checkoutDetails.copy(successfulTimeStamp = timeStamp, selectedPaymentMethod = paymentMethod)
        _checkoutDetailsFlow.value = checkoutDetails
    }
}