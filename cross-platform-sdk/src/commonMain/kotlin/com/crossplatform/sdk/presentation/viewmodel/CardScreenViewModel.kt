package com.crossplatform.sdk.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AnalyticsEvents
import com.crossplatform.sdk.data.model.FetchCardDetails
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import com.crossplatform.sdk.domain.repo.CardScreenRepo
import com.crossplatform.sdk.domain.repo.FetchStatusRepo
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
import kotlinx.coroutines.launch

class CardScreenViewModel(
    private val repo : CardScreenRepo,
    private val fetchStatusRepo : FetchStatusRepo,
    private val analyticsRepo : CallUIAnalyticsRepo
) : ViewModel() {

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

    var emiIssuerExist = mutableStateOf(true)
    var emiIssuer      =  mutableStateOf("")

    var isSavedCardCheckBoxClicked =  mutableStateOf(false)
    var showCvvInfo                =  mutableStateOf(false)
    var showKnowMoreDialog         =  mutableStateOf(false)

    fun fetchCardDetails(cardNumber : String, isTestEnv: Boolean) {
        viewModelScope.launch {
            when(val response = repo.getCardDetails(cardNumber)) {
                is ApiResponse.Success<*> -> {
                    val data = response.data as FetchCardDetails
                    println("-======data $data")
                    updateCardIcon(isTestEnv, data.paymentMethod.brand)
                    _cardDetails.value = UiState.Success(data)
                }
                is ApiResponse.Error->  {
                    println("==============error ${response.errorBody}")
                }
                else -> {
                    println("==============error")
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
    fun checkCardValid(isTestEnv : Boolean, isEmiFlow : Boolean) {
        val numberLen = if (maxCardNumberLength.value == 19) 16 else 15
        val baseValid =
            !cardNumberError.value && !cardExpiryError.value && !cardCvvError.value && !cardHolderNameError.value &&
                    (cardNumberText.value.replace(" ", "").length == numberLen || isTestEnv) &&
                    cardExpiryText.value.length == 4 &&
                    cardCvvText.value.length == maxCvvLength.value &&
                    cardHolderNameText.value.isNotEmpty()
        cardValid.value = if (isEmiFlow) baseValid && emiIssuerExist.value else baseValid
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

        checkCardValid(isTestEnv, false)

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
        checkCardValid(isTestEnv, false)
    }

    fun postCardRequest() {
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
            val response = repo.postCardDetails(
                type = "card/plain",
                cardNumber = cardNumberText.value,
                cardName = cardHolderNameText.value,
                expiry = formatExpiryForApi(cardExpiryText.value),
                cvv = cardCvvText.value,
                nickName = cardNickNameText.value,
                isSaveInstrumentCheckboxClicked = isSavedCardCheckBoxClicked.value,
                isSICheckboxClicked = false
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
                onOpenQr = {
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

    fun callFetchStatus(inquiryResult : String) {
        viewModelScope.launch {
            CheckoutDetailsHandler.setInquiryToken(inquiryResult)
            isBoxPayAnimationVisible.value = true
            val response = fetchStatusRepo.fetchStatus()
            handleFetchStatus(
                response = response,
                setIsBoxPayAnimationVisible = {isBoxPayAnimationVisible.value = it}
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

    fun callUiAnalytics(
        event : String,
        screenName : String,
        message : String
    ) {
        viewModelScope.launch {
            analyticsRepo.callUiAnalytics(event, screenName, message)
        }
    }
}