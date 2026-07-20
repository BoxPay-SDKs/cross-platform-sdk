package com.crossplatform.sdk.data.handler

import com.crossplatform.sdk.data.model.CheckoutDetails
import com.crossplatform.sdk.domain.model.SurchargeModel
import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


object CheckoutDetailsHandler {

    // ─── Scope ────────────────────────────────────────────────────────────────
    // SupervisorJob: one child failing doesn't cancel siblings
    // Main.immediate: state reads happen on main thread without frame delay
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // ─── Default ──────────────────────────────────────────────────────────────
    private fun defaultCheckoutDetails() = CheckoutDetails(
        currencySymbol = "",
        currencyCode = "",
        amountBeforeSurcharge = 0.0,
        discountAmount = 0.0,
        amount = 0.0,
        token = "",
        isSessionExpired = false,
        isPaymentFailed = false,
        successfulTimeStamp = "",
        selectedPaymentMethod = "",
        isPaymentSuccessful = false,
        merchantLogo = "",
        merchantName = "",
        isTestEnv = false,
        itemsLength = 0,
        errorMessage = "",
        shopperToken = null,
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
        isSICheckboxEnabled = false,
        isSICheckboxChecked = false,
        isSubscriptionCheckout = false,
        status = TransactionStatusEnum.NOACTION.name,
        transactionId = "",
        inquiryToken = "",
        isSuccessScreenVisible = false,
        isFailedScreenVisible = false,
        showQROnLoad = false,
        isMerchantLogoVisible = false,
        isSessionExpiryVisible = false,
        surchargeDetails = emptyList(),
        isWebViewVisible = false,
        appliedOfferId = null,
        subscription = null,
        // UI Configuration
        ctaBorderRadius = 0,
        buttonColor = "",
        buttonTextColor = "",
        headerColor = "",
        headerTextColor = "",
        focusedTextInputBorderColor = "",
        unfocusedTextInputBorderColor = "",
        fontFamily = "",
        ctaTextFontSize = 0,
        acceptedCardsList = emptyList(),
        showRetryBottomDown = false,
        proceedAutoRetryPayment = {}
    )

    // ─── Source of truth ──────────────────────────────────────────────────────
    private val _checkoutDetailsFlow = MutableStateFlow(defaultCheckoutDetails())

    var checkoutDetails: CheckoutDetails = defaultCheckoutDetails()
        private set

    // ─── Derived StateFlows ───────────────────────────────────────────────────
    // Each composable subscribes to only the field it needs.
    // distinctUntilChanged() ensures emission only when the value actually changes,
    // so a setAmount() call won't recompose a composable reading merchantName.

    // -- Amount & Currency
    val amountFlow: StateFlow<Double> = _checkoutDetailsFlow
        .map { it.amount }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultCheckoutDetails().amount)

    val amountBeforeSurchargeFlow: StateFlow<Double> = _checkoutDetailsFlow
        .map { it.amountBeforeSurcharge }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultCheckoutDetails().amountBeforeSurcharge)

    val discountAmountFlow: StateFlow<Double> = _checkoutDetailsFlow
        .map { it.discountAmount }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultCheckoutDetails().discountAmount)

    val currencyFlow: StateFlow<Pair<String, String>> = _checkoutDetailsFlow
        .map { it.currencySymbol to it.currencyCode }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, "" to "")

    val surchargeDetailsFlow: StateFlow<List<SurchargeModel>> = _checkoutDetailsFlow
        .map { it.surchargeDetails }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    // -- Merchant branding
    val merchantNameFlow: StateFlow<String> = _checkoutDetailsFlow
        .map { it.merchantName }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultCheckoutDetails().merchantName)

    val proceedAutoRetryFunctionFlow : StateFlow<Function<Unit>> = _checkoutDetailsFlow
        .map { it.proceedAutoRetryPayment }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultCheckoutDetails().proceedAutoRetryPayment)

    val showRetryBottomDownFlow: StateFlow<Boolean> = _checkoutDetailsFlow
        .map { it.showRetryBottomDown }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultCheckoutDetails().showRetryBottomDown)

    val acceptedCardsListFlow: StateFlow<List<String>> = _checkoutDetailsFlow
        .map { it.acceptedCardsList }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultCheckoutDetails().acceptedCardsList)

    val merchantLogoFlow: StateFlow<String> = _checkoutDetailsFlow
        .map { it.merchantLogo }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultCheckoutDetails().merchantLogo)

    val isMerchantLogoVisibleFlow: StateFlow<Boolean> = _checkoutDetailsFlow
        .map { it.isMerchantLogoVisible }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultCheckoutDetails().isMerchantLogoVisible)

    // -- Theme / styling
    val buttonColorFlow: StateFlow<String> = _checkoutDetailsFlow
        .map { it.buttonColor }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultCheckoutDetails().buttonColor)

    val buttonTextColorFlow: StateFlow<String> = _checkoutDetailsFlow
        .map { it.buttonTextColor }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultCheckoutDetails().buttonTextColor)

    val ctaBorderRadiusFlow: StateFlow<Int> = _checkoutDetailsFlow
        .map { it.ctaBorderRadius }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultCheckoutDetails().ctaBorderRadius)

    val focusedBorderColorFlow: StateFlow<String> = _checkoutDetailsFlow
        .map { it.focusedTextInputBorderColor }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultCheckoutDetails().focusedTextInputBorderColor)

    val unfocusedBorderColorFlow: StateFlow<String> = _checkoutDetailsFlow
        .map { it.unfocusedTextInputBorderColor }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, defaultCheckoutDetails().unfocusedTextInputBorderColor)

    val isPaymentSuccessfulFlow: StateFlow<Boolean> = _checkoutDetailsFlow
        .map { it.isPaymentSuccessful }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    val isPaymentFailedFlow: StateFlow<Boolean> = _checkoutDetailsFlow
        .map { it.isPaymentFailed }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    val isSessionExpiredFlow: StateFlow<Boolean> = _checkoutDetailsFlow
        .map { it.isSessionExpired }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    val transactionFlow: StateFlow<Pair<String, String>> = _checkoutDetailsFlow
        .map { it.status to it.transactionId }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, TransactionStatusEnum.NOACTION.name to "")

    val inquiryTokenFlow: StateFlow<String> = _checkoutDetailsFlow
        .map { it.inquiryToken }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, "")

    val errorMessageFlow: StateFlow<String> = _checkoutDetailsFlow
        .map { it.errorMessage }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, "")

    val isWebViewVisibleFlow: StateFlow<Boolean> = _checkoutDetailsFlow
        .map { it.isWebViewVisible }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    // -- Screen visibility
    val isSuccessScreenVisibleFlow: StateFlow<Boolean> = _checkoutDetailsFlow
        .map { it.isSuccessScreenVisible }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    val isFailedScreenVisibleFlow: StateFlow<Boolean> = _checkoutDetailsFlow
        .map { it.isFailedScreenVisible }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    val isSessionExpiryVisibleFlow: StateFlow<Boolean> = _checkoutDetailsFlow
        .map { it.isSessionExpiryVisible }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    val showQROnLoadFlow: StateFlow<Boolean> = _checkoutDetailsFlow
        .map { it.showQROnLoad }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    // -- Token & env
    val tokenFlow: StateFlow<String> = _checkoutDetailsFlow
        .map { it.token }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, "")

    val shopperTokenFlow: StateFlow<String?> = _checkoutDetailsFlow
        .map { it.shopperToken }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)

    val isTestEnvFlow: StateFlow<Boolean> = _checkoutDetailsFlow
        .map { it.isTestEnv }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    val fontFamilyFlow: StateFlow<String> = _checkoutDetailsFlow
        .map { it.fontFamily }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, "")

    val ctaTextFontSizeFlow: StateFlow<Int> = _checkoutDetailsFlow
        .map { it.ctaTextFontSize }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, 0)

    // -- SI / Subscription
    val isSICheckboxCheckedFlow: StateFlow<Boolean> = _checkoutDetailsFlow
        .map { it.isSICheckboxChecked }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    val isSICheckboxEnabledFlow: StateFlow<Boolean> = _checkoutDetailsFlow
        .map { it.isSICheckboxEnabled }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    val isSubscriptionCheckoutFlow: StateFlow<Boolean> = _checkoutDetailsFlow
        .map { it.isSubscriptionCheckout }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    val subscriptionFlow: StateFlow<List<Pair<String, String>>?> = _checkoutDetailsFlow
        .map { it.subscription }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, null)

    // -- Shopper fields config (grouped — all change together from setSDKConfig)
    val shopperFieldsConfigFlow: StateFlow<ShopperFieldsConfig> = _checkoutDetailsFlow
        .map {
            ShopperFieldsConfig(
                isShippingAddressEnabled  = it.isShippingAddressEnabled,
                isShippingAddressEditable = it.isShippingAddressEditable,
                isFullNameEnabled         = it.isFullNameEnabled,
                isFullNameEditable        = it.isFullNameEditable,
                isEmailEnabled            = it.isEmailEnabled,
                isEmailEditable           = it.isEmailEditable,
                isPhoneEnabled            = it.isPhoneEnabled,
                isPhoneEditable           = it.isPhoneEditable,
                isPanEnabled              = it.isPanEnabled,
                isPanEditable             = it.isPanEditable,
                isDOBEnabled              = it.isDOBEnabled,
                isDOBEditable             = it.isDOBEditable,
            )
        }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, ShopperFieldsConfig())

    // -- Order display
    val isOrderItemDetailsVisibleFlow: StateFlow<Boolean> = _checkoutDetailsFlow
        .map { it.isOrderItemDetailsVisible }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, true)

    val itemsLengthFlow: StateFlow<Int> = _checkoutDetailsFlow
        .map { it.itemsLength }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, 0)

    val successfulTimestampFlow: StateFlow<Pair<String, String>> = _checkoutDetailsFlow
        .map { it.successfulTimeStamp to it.selectedPaymentMethod }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, "" to "")

    // ─── Setters (unchanged from your original) ───────────────────────────────

    fun set(details: CheckoutDetails) {
        checkoutDetails = details
        _checkoutDetailsFlow.value = details
    }

    fun setStatusAndTransID(status: String, transactionId: String) {
        checkoutDetails = checkoutDetails.copy(status = status, transactionId = transactionId)
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setStatus(status : String) {
        checkoutDetails = checkoutDetails.copy(status = status)
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setInquiryToken(inquiryToken: String) {
        checkoutDetails = checkoutDetails.copy(inquiryToken = inquiryToken)
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setSDKConfig(
        currencySymbol: String,
        currencyCode: String,
        amountBeforeSurcharge: Double,
        amount: Double,
        buttonColor: String,
        buttonTextColor: String,
        headerColor: String,
        headerTextColor: String,
        merchantName: String,
        merchantLogo: String,
        itemsLength: Int,
        errorMessage: String,
        isShippingAddressEnabled: Boolean,
        isShippingAddressEditable: Boolean,
        isFullNameEnabled: Boolean,
        isFullNameEditable: Boolean,
        isEmailEnabled: Boolean,
        isEmailEditable: Boolean,
        isPhoneEnabled: Boolean,
        isPhoneEditable: Boolean,
        isPanEnabled: Boolean,
        isPanEditable: Boolean,
        isDOBEnabled: Boolean,
        isDOBEditable: Boolean,
        isOrderItemDetailsVisible: Boolean,
        isSubscriptionCheckout: Boolean,
        isMerchantLogoVisible: Boolean,
        isSessionExpiryVisible: Boolean,
        subscription: List<Pair<String, String>>?,
        fontFamily : String,
        ctaTextFontSize : Int,
        inputBorderColor : String,
        inputFocusBorderColor : String,
        ctaBorderRadius: Int,
        acceptedCardList : List<String>
    ) {
        checkoutDetails = checkoutDetails.copy(
            currencySymbol            = currencySymbol,
            currencyCode              = currencyCode,
            amountBeforeSurcharge     = amountBeforeSurcharge,
            amount                    = amount,
            buttonColor               = buttonColor,
            buttonTextColor           = buttonTextColor,
            headerColor               = headerColor,
            headerTextColor           = headerTextColor,
            itemsLength               = itemsLength,
            merchantName              = merchantName,
            merchantLogo              = merchantLogo,
            errorMessage              = errorMessage,
            isShippingAddressEnabled  = isShippingAddressEnabled,
            isShippingAddressEditable = isShippingAddressEditable,
            isFullNameEnabled         = isFullNameEnabled,
            isFullNameEditable        = isFullNameEditable,
            isEmailEnabled            = isEmailEnabled,
            isEmailEditable           = isEmailEditable,
            isPhoneEnabled            = isPhoneEnabled,
            isPhoneEditable           = isPhoneEditable,
            isPanEnabled              = isPanEnabled,
            isPanEditable             = isPanEditable,
            isDOBEnabled              = isDOBEnabled,
            isDOBEditable             = isDOBEditable,
            isOrderItemDetailsVisible = isOrderItemDetailsVisible,
            isSubscriptionCheckout    = isSubscriptionCheckout,
            isSessionExpiryVisible    = isSessionExpiryVisible,
            isMerchantLogoVisible     = isMerchantLogoVisible,
            subscription              = subscription,
            ctaTextFontSize = ctaTextFontSize,
            fontFamily = fontFamily,
            focusedTextInputBorderColor = inputFocusBorderColor,
            unfocusedTextInputBorderColor = inputBorderColor,
            ctaBorderRadius = ctaBorderRadius,
            acceptedCardsList = acceptedCardList
        )
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun resetToDefault() {
        checkoutDetails = defaultCheckoutDetails()
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setCheckoutToken(
        shopperToken: String?,
        token: String,
        isTestEnv: Boolean,
        isSuccessScreenVisible: Boolean,
        ctaBorderRadius: Int,
        isSICheckboxChecked: Boolean,
        isSICheckboxEnabled: Boolean,
        isFailedScreenVisible: Boolean,
        showQROnLoad: Boolean,
        focusedTextInputBorderColor: String,
        unfocusedTextInputBorderColor: String
    ) {
        checkoutDetails = checkoutDetails.copy(
            shopperToken                  = shopperToken,
            token                         = token,
            isTestEnv                     = isTestEnv,
            isSuccessScreenVisible        = isSuccessScreenVisible,
            ctaBorderRadius               = ctaBorderRadius,
            isSICheckboxChecked           = isSICheckboxChecked,
            isSICheckboxEnabled           = isSICheckboxEnabled,
            isFailedScreenVisible         = isFailedScreenVisible,
            focusedTextInputBorderColor   = focusedTextInputBorderColor,
            unfocusedTextInputBorderColor = unfocusedTextInputBorderColor,
            showQROnLoad                  = showQROnLoad
        )
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setCheckoutTokenForBoxPayElements(
        shopperToken: String?,
        token: String,
        isTestEnv: Boolean,
        ctaBorderRadius: Int,
        isSICheckboxChecked: Boolean,
        isSICheckboxEnabled: Boolean,
        focusedTextInputBorderColor: String,
        unfocusedTextInputBorderColor: String,
        showQROnLoad: Boolean
    ) {
        checkoutDetails = checkoutDetails.copy(
            shopperToken                  = shopperToken,
            token                         = token,
            isTestEnv                     = isTestEnv,
            ctaBorderRadius               = ctaBorderRadius,
            isSICheckboxChecked           = isSICheckboxChecked,
            isSICheckboxEnabled           = isSICheckboxEnabled,
            focusedTextInputBorderColor   = focusedTextInputBorderColor,
            unfocusedTextInputBorderColor = unfocusedTextInputBorderColor,
            showQROnLoad = showQROnLoad
        )
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setSessionExpired() {
        checkoutDetails = checkoutDetails.copy(isSessionExpired = !checkoutDetails.isSessionExpired)
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

    fun setErrorMessage(
        message: String = "You may have cancelled the payment or there was a delay in response. Please retry."
    ) {
        checkoutDetails = checkoutDetails.copy(errorMessage = message)
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun showAutoRetryDropDown(proceedAutoRetry : () -> Unit) {
        checkoutDetails = checkoutDetails.copy(showRetryBottomDown = true, proceedAutoRetryPayment = proceedAutoRetry)
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun hideAutoRetryDropDown() {
        checkoutDetails = checkoutDetails.copy(showRetryBottomDown = false)
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setTimeAndPaymentMethod(timeStamp: String, paymentMethod: String) {
        checkoutDetails = checkoutDetails.copy(
            successfulTimeStamp   = timeStamp,
            selectedPaymentMethod = paymentMethod
        )
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setSurchargeDetails(surchargeDetails: List<SurchargeModel>) {
        checkoutDetails = checkoutDetails.copy(surchargeDetails = surchargeDetails)
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setIsWebViewVisible(showWebView: Boolean) {
        checkoutDetails = checkoutDetails.copy(isWebViewVisible = showWebView)
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setAmount(amount: Double) {
        checkoutDetails = checkoutDetails.copy(amount = amount)
        _checkoutDetailsFlow.value = checkoutDetails
    }

    fun setAppliedOffer(appliedOfferId: String, amount: Double) {
        checkoutDetails = checkoutDetails.copy(
            appliedOfferId = appliedOfferId,
            discountAmount = amount
        )
        _checkoutDetailsFlow.value = checkoutDetails
    }
}

// ─── Shopper fields config — grouped data class ───────────────────────────────
// Keeps shopperFieldsConfigFlow clean instead of 12 separate flows
data class ShopperFieldsConfig(
    val isShippingAddressEnabled  : Boolean = false,
    val isShippingAddressEditable : Boolean = false,
    val isFullNameEnabled         : Boolean = false,
    val isFullNameEditable        : Boolean = false,
    val isEmailEnabled            : Boolean = false,
    val isEmailEditable           : Boolean = false,
    val isPhoneEnabled            : Boolean = false,
    val isPhoneEditable           : Boolean = false,
    val isPanEnabled              : Boolean = false,
    val isPanEditable             : Boolean = false,
    val isDOBEnabled              : Boolean = false,
    val isDOBEditable             : Boolean = false,
)