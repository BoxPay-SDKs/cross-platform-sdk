package com.crossplatform.sdk.presentation.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AnalyticsEvents
import com.crossplatform.sdk.data.model.FetchCardDetails
import com.crossplatform.sdk.domain.mapper.toUiModel
import com.crossplatform.sdk.domain.model.Bank
import com.crossplatform.sdk.domain.model.ChooseEmiModel
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import com.crossplatform.sdk.domain.repo.CardScreenRepo
import com.crossplatform.sdk.domain.repo.FetchStatusRepo
import com.crossplatform.sdk.domain.repo.OtherPaymentMethodRepo
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.currentMonth
import com.crossplatform.sdk.presentation.currentYear
import com.crossplatform.sdk.presentation.sharedContext.handleFetchStatus
import com.crossplatform.sdk.presentation.sharedContext.handlePaymentResponse
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_amex
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_card
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_maestro
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_masterCard
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_rupay
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_visa
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class EmiStep { Content, Tenure, Card }

class EMIScreenViewModel(
    private val repo : OtherPaymentMethodRepo,
    private val cardScreenRepo: CardScreenRepo,
    private val analyticsRepo : CallUIAnalyticsRepo,
    private val fetchStatusRepo: FetchStatusRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ChooseEmiModel>>(UiState.Loading)
    val uiState : StateFlow<UiState<ChooseEmiModel>> get() = _uiState

    private val steps = mutableStateListOf(EmiStep.Content)
    val currentStep: EmiStep get() = steps.last()
    fun goToTenure() { steps.add(EmiStep.Tenure) }
    fun goToCard()   { steps.add(EmiStep.Card) }
    val emiBankList = mutableStateOf(ChooseEmiModel(emptyList()))
    val selectedCard = mutableStateOf("Credit Card")
    val offerSelectedCode = mutableStateOf<String?>(null)

    val selectedEmi = mutableStateOf<Pair<Int?, String?>>(Pair(null, null))
    val searchText = mutableStateOf("")
    val selectedFilter = mutableStateOf("")

    val selectedOthers = mutableStateOf<String?>(null)
    val selectedBank = mutableStateOf(null as Bank?)

    val isNoCostSelected = mutableStateOf(false)
    val isLowCostSelected = mutableStateOf(false)
    val discount = mutableStateOf<String?>(null)
    val netAmount = mutableStateOf<String?>(null)
    val selectedPercent = mutableStateOf<Double?>(null)


    init {
        loadPaymentMethods()
    }

    fun callUiAnalytics(
        event : String,
        screenName : String,
        message : String
    ) {
        viewModelScope.launch {
            analyticsRepo.callUiAnalytics(event, screenName, message)
        }
    }

    private fun loadPaymentMethods() {
        viewModelScope.launch {
            val checkoutDetails = CheckoutDetailsHandler.checkoutDetails
            val response = repo.getPaymentMethods(
                amount = if (checkoutDetails.appliedOfferId.isNullOrBlank()) checkoutDetails.amount else null,
                offerId = checkoutDetails.appliedOfferId
            )
            _uiState.value = when (response) {
                is ApiResponse.Error -> {
                    UiState.Error(response.message)
                }
                ApiResponse.Loading -> {
                    UiState.Loading
                }
                is ApiResponse.Success-> {
                    val uiModel = response.data.toUiModel()
                    emiBankList.value = uiModel
                    UiState.Success(uiModel)
                }
            }
        }
    }

    fun onClickCard(cardType : String) {
        selectedCard.value = cardType
        searchText.value = ""
        selectedFilter.value = ""
    }

    fun onEditSearchText(text : String) {
        searchText.value = text
    }

    fun onToggleFilter(filter : String) {
        selectedFilter.value = if (selectedFilter.value == filter) "" else filter
    }

    fun onSelectedOthersOption(selected : String) {
        selectedOthers.value = selected
    }

    fun onProceedWithOther() {

    }

    fun onClickBank(bank : Bank) {
        selectedBank.value = if (selectedBank.value?.name == bank.name) null else bank
        goToTenure()
    }

    fun onClickRadio(duration: Int, amount: String, code: String?) {
        selectedEmi.value = Pair(duration, amount)
        offerSelectedCode.value = code
    }

    fun onProceedEmi(percent: Double, isLowCost: Boolean, isNoCost : Boolean, dis: String, net: String) {
        selectedPercent.value = percent
        isLowCostSelected.value = isLowCost
        isNoCostSelected.value = isNoCost
        discount.value = dis
        netAmount.value = net
        goToCard()
    }


    val isBoxPayAnimationVisible = MutableStateFlow(false)
    val showWebview = MutableStateFlow(false)
    val url = mutableStateOf<String?>(null)
    val htmlString = mutableStateOf<String?>(null)

    private val _cardDetails = MutableStateFlow<UiState<FetchCardDetails>>(UiState.Loading)

    // --- Field States ---
    var cardNumberText     =  mutableStateOf("")
    var cardHolderNameText =  mutableStateOf("")
    var cardExpiryText     =  mutableStateOf("")
    var cardCvvText        =  mutableStateOf("")
    var cardNickNameText   =  mutableStateOf("")

    // --- Validation States ---
    var cardNumberError       = mutableStateOf(false)
    var cardHolderNameError   =  mutableStateOf(false)
    var cardExpiryError       = mutableStateOf(false)
    var cardCvvError          =  mutableStateOf(false)

    var cardNumberErrorText     = mutableStateOf("Required")
    var cardHolderNameErrorText = mutableStateOf("Required")
    var cardExpiryErrorText     = mutableStateOf("Required")
    var cardCvvErrorText        = mutableStateOf("Required")

    var cardNumberValid  =  mutableStateOf(true)
    var cardExpiryValid  =  mutableStateOf(true)
    var methodEnabled    =  mutableStateOf(true)
    var cardValid        =  mutableStateOf(false)

    var maxCvvLength        = mutableStateOf(3)
    var maxCardNumberLength =  mutableStateOf(19)
    var cardSelectedIcon    = mutableStateOf(Res.drawable.ic_card)


    var isSavedCardCheckBoxClicked =  mutableStateOf(false)
    var showCvvInfo                =  mutableStateOf(false)
    var showKnowMoreDialog         =  mutableStateOf(false)

    fun fetchCardDetails(cardNumber : String, isTestEnv: Boolean) {
        viewModelScope.launch {
            when(val response = cardScreenRepo.getCardDetails(cardNumber)) {
                is ApiResponse.Success<*> -> {
                    val data = response.data as FetchCardDetails
                    updateCardIcon(isTestEnv, data.paymentMethod.brand)
                    _cardDetails.value = UiState.Success(data)
                }
                else -> {
                    // no instructions to be performed
                }
            }

        }
    }

    // --- Luhn Validation ---
    fun isValidCardNumberByLuhn(number: String): Boolean {
        if (number.length < 13) return false
        var sum          = 0
        var isSecondDigit = false
        for (i in number.indices.reversed()) {
            var d = number[i].digitToInt()
            if (isSecondDigit) d *= 2
            sum          += d / 10
            sum          += d % 10
            isSecondDigit = !isSecondDigit
        }
        return sum % 10 == 0
    }

    // --- Card Validity Check ---
    fun checkCardValid(isTestEnv : Boolean) {
        val numberLen = if (maxCardNumberLength.value == 19) 16 else 15
        val baseValid =
            !cardNumberError.value && !cardExpiryError.value && !cardCvvError.value && !cardHolderNameError.value &&
                    (cardNumberText.value.replace(" ", "").length == numberLen || isTestEnv) &&
                    cardExpiryText.value.length == 4 &&
                    cardCvvText.value.length == maxCvvLength.value &&
                    cardHolderNameText.value.isNotEmpty()
        cardValid.value = baseValid
    }

    // --- Handle Card Number Change ---
    fun handleCardNumberChange(text: String, isTestEnv: Boolean) {
        // ← only store raw digits, no formatting
        val cleaned = text.filter { it.isDigit() }.take(16)
        cardNumberText.value = cleaned

        if (cleaned.isEmpty()) {
            cardSelectedIcon.value    = Res.drawable.ic_card
            maxCvvLength.value        = 3
            maxCardNumberLength.value = 19
            return
        }

        checkCardValid(isTestEnv)

        if (cleaned.length == 16 || (cleaned.length == 15 && maxCardNumberLength.value == 15)) {
            cardNumberValid.value = isValidCardNumberByLuhn(cleaned)
        }

        if (cleaned.length == 9) {
            fetchCardDetails(cardNumber = cleaned.take(9), isTestEnv)
        }

        if (cleaned.length < 9) {
            cardSelectedIcon.value    = Res.drawable.ic_card
            maxCvvLength.value        = 3
            maxCardNumberLength.value = 19
        }
    }

    // --- Update icon from API ---
    fun updateCardIcon(isTestEnv: Boolean, brand : String) {
        when (brand) {
            "VISA"            -> { cardSelectedIcon.value = Res.drawable.ic_visa;       maxCvvLength.value = 3; maxCardNumberLength.value = 19 }
            "Mastercard"      -> { cardSelectedIcon.value = Res.drawable.ic_masterCard; maxCvvLength.value = 3; maxCardNumberLength.value = 19 }
            "RUPAY"           -> { cardSelectedIcon.value = Res.drawable.ic_rupay;      maxCvvLength.value = 3; maxCardNumberLength.value = 19 }
            "AmericanExpress" -> { cardSelectedIcon.value = Res.drawable.ic_amex;       maxCvvLength.value = 4; maxCardNumberLength.value = if (isTestEnv) 19 else 18 }
            "Maestro"         -> { cardSelectedIcon.value = Res.drawable.ic_maestro;    maxCvvLength.value = 3; maxCardNumberLength.value = 19 }
            else              -> { cardSelectedIcon.value = Res.drawable.ic_card;       maxCvvLength.value = 3; maxCardNumberLength.value = 19 }
        }
    }

    // --- Handle Expiry Change ---
    fun handleExpiryChange(text: String, isTestEnv: Boolean) {
        // ← only store raw digits, no slash
        val cleaned = text.filter { it.isDigit() }.take(4)

        // Auto prefix 0 for month > 1
        val adjusted = if (cleaned.length == 1 && cleaned.toInt() > 1) "0$cleaned"
        else cleaned

        cardExpiryText.value = adjusted   // ← store "MMYY" not "MM/YY"

        var monthError = false
        var yearError  = false

        if (adjusted.length >= 2) {
            val month = adjusted.take(2).toInt()
            monthError = month !in 1..12
        }
        if (adjusted.length == 4) {
            val curYear   = currentYear()
            val curMonth  = currentMonth()
            val enteredMonth = adjusted.take(2).toInt()
            val enteredYear  = adjusted.drop(2).toInt()
            yearError = enteredYear < curYear ||
                    (enteredYear == curYear && enteredMonth < curMonth)
        }
        cardExpiryValid.value = !monthError && !yearError
        checkCardValid(isTestEnv)
    }

    fun callFetchStatus(inquiryResult : String) {
        viewModelScope.launch {
            CheckoutDetailsHandler.setInquiryToken(inquiryResult)
            isBoxPayAnimationVisible.value = true
            val response = fetchStatusRepo.fetchStatus()
            handleFetchStatus(
                response = response,
                setIsBoxPayAnimationVisible = {isBoxPayAnimationVisible.value = it},
                onAutoRetry = {
                    CheckoutDetailsHandler.showAutoRetryDropDown { autoRetryInitiatePayment() }
                    isBoxPayAnimationVisible.value = false
                }
            )
        }
    }

    fun autoRetryInitiatePayment() {
        viewModelScope.launch {
            isBoxPayAnimationVisible.value = true
            val checkoutDetails = CheckoutDetailsHandler.checkoutDetails
            val response = fetchStatusRepo.autoRetryInitiatePayment(checkoutDetails.transactionId)
            handlePaymentResponse(
                response = response,
                onSetPaymentUrl = {
                    url.value = it
                    setWebViewScreen(true)
                },
                onSetPaymentHtml = {
                    htmlString.value = it
                    setWebViewScreen(true)
                },
                onNavigateToTimer = {
                    // no operation
                },
                onOpenQr = {_, _ ->
                    // no operation
                },
                onOpenUpiIntent = {_ ->
                    // no operation
                },
                errorMessage = CheckoutDetailsHandler.checkoutDetails.errorMessage,
                setIsBoxPayAnimationVisible = {
                    isBoxPayAnimationVisible.value = it
                }
            )
        }
    }

    fun formatExpiryForApi(expiry: String): String {
        if (expiry.length != 4) return ""
        val month = expiry.take(2)       // "05"
        val year  = "20${expiry.drop(2)}" // "30" → "2030"
        return "$year-$month"            // "2030-05"
    }

    fun setWebViewScreen(boolean: Boolean) {
        showWebview.value = boolean
        CheckoutDetailsHandler.setIsWebViewVisible(boolean)
    }

    fun goBackStep(): Boolean {
        if (steps.size <= 1) return false
        steps.removeAt(steps.lastIndex)
        return true
    }

    fun postEMIRequest() {
        viewModelScope.launch {
            callUiAnalytics(
                event = AnalyticsEvents.PAYMENT_CATEGORY_SELECTED.value,
                screenName = "CardScreenViewModel",
                message = "payment category selected"
            )
            callUiAnalytics(
                event = AnalyticsEvents.PAYMENT_INITIATED.value,
                screenName = "CardScreenViewModel",
                message = "payment initiated"
            )
            isBoxPayAnimationVisible.value = true
            val response = repo.initiateEMIPayment(
                cardType = selectedCard.value,
                cardNumber = cardNumberText.value,
                holderName = cardHolderNameText.value,
                expiryDate = formatExpiryForApi(cardExpiryText.value),
                cvv = cardCvvText.value,
                duration = selectedEmi.value.first,
                provider = selectedOthers.value,
                offerCode = offerSelectedCode.value
            )
            handlePaymentResponse(
                response = response,
                onSetPaymentHtml = {html ->
                    htmlString.value = html
                    setWebViewScreen(true)
                },
                onOpenUpiIntent = {
                    // no operations
                },
                onNavigateToTimer = {
                    // no operations
                },
                onOpenQr = {_,_ ->
                    // no operations
                },
                onSetPaymentUrl = {responseUrl ->
                    url.value = responseUrl
                    setWebViewScreen(true)
                },
                setIsBoxPayAnimationVisible = {isBoxPayAnimationVisible.value = it},
                errorMessage = CheckoutDetailsHandler.checkoutDetails.errorMessage
            )
        }
    }
}