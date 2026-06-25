package com.crossplatform.sdk.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.handler.UserDataHandler
import com.crossplatform.sdk.data.model.AnalyticsEvents
import com.crossplatform.sdk.data.model.FetchCardDetails
import com.crossplatform.sdk.domain.mapper.toUiModel
import com.crossplatform.sdk.domain.model.AppLifecycleState
import com.crossplatform.sdk.domain.model.CountryDetailsModel
import com.crossplatform.sdk.domain.model.MainScreenModel
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import com.crossplatform.sdk.domain.repo.CardScreenRepo
import com.crossplatform.sdk.domain.repo.MainScreenRepo
import com.crossplatform.sdk.domain.repo.OtherPaymentMethodRepo
import com.crossplatform.sdk.presentation.AppLifecycleObserver
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.components.upiRegex
import com.crossplatform.sdk.presentation.currentMonth
import com.crossplatform.sdk.presentation.currentYear
import com.crossplatform.sdk.presentation.loadCountryData
import com.crossplatform.sdk.presentation.sharedContext.handleFetchStatus
import com.crossplatform.sdk.presentation.sharedContext.handlePaymentResponse
import com.crossplatform.sdk.presentation.sharedContext.handleUpiCollectFetchStatus
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_amex
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_card
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_maestro
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_masterCard
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_rupay
import crossplatformsdk.cross_platform_sdk.generated.resources.ic_visa
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class BoxPayElementsViewModel (
    private val repo: MainScreenRepo,
    private val cardRepo : CardScreenRepo,
    private val analyticsRepo : CallUIAnalyticsRepo,
    private val otherPaymentMethodRepo: OtherPaymentMethodRepo
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<MainScreenModel>>(UiState.Loading)
    val state: StateFlow<UiState<MainScreenModel>> = _state
    private var isSessionLoaded = false

    val isToastVisible = MutableStateFlow(false)
    private var countryData: Map<String, CountryDetailsModel> = emptyMap()
    private var countdownJob: Job? = null
    val isBoxPayAnimationLoading = MutableStateFlow(false)
    val setWebViewUrl = mutableStateOf("")
    val setWebViewHtml = mutableStateOf("")
    val proceedToTimer = mutableStateOf(false)
    val upiId = mutableStateOf("")
    val upiIntentUrl = mutableStateOf("")

    val isUpiOpening = mutableStateOf(false)

    val showWebViewScreen = MutableStateFlow(false)

    private var fetchStatusJob: Job? = null

    val recommendedList = mutableStateOf<List<SelectedPaymentMethod>>(emptyList())
    val upiRecommendedList = mutableStateOf<List<SelectedPaymentMethod>>(emptyList())
    val cardsRecommendedList = mutableStateOf<List<SelectedPaymentMethod>>(emptyList())

    val qrTimer = mutableStateOf(0)
    val qrImage = mutableStateOf("")

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

    private val allBanks = mutableStateOf<List<SelectedPaymentMethod>>(emptyList())
    val netBankingSearchQuery = mutableStateOf("")

    private val _uiState = MutableStateFlow<UiState<List<SelectedPaymentMethod>>>(UiState.Loading)
    val uiState : StateFlow<UiState<List<SelectedPaymentMethod>>> get() = _uiState
    private fun cardValidFlow(): Flow<Boolean> = snapshotFlow { cardValid.value }



    private val lifecycleObserver = AppLifecycleObserver { state ->
        if (state == AppLifecycleState.Foreground && isUpiOpening.value) {
            upiIntentUrl.value = ""
            callFetchStatus("")
        }
    }

    val isQRLoaded = mutableStateOf(false)

    init {
        loadSession()
    }

    fun loadSession() {
        if (isSessionLoaded) return
        val checkoutDetails = CheckoutDetailsHandler.checkoutDetails
        if (checkoutDetails.token.isEmpty()) {
            _state.value = UiState.Error("Token is empty")
            return
        }

        viewModelScope.launch {
            try {
                countryData = loadCountryData()

                val sessionResponse = repo.getSessionDetails()
                if (sessionResponse !is ApiResponse.Success) {
                    _state.value = when (sessionResponse) {
                        is ApiResponse.Error -> UiState.Error(
                            sessionResponse.errorBody ?: sessionResponse.message
                        )
                        else -> UiState.Loading
                    }
                    return@launch
                }

                val sessionData = sessionResponse.data
                val responseData = sessionData.toUiModel()
                val shopper = sessionData.paymentDetails.shopper

                if (responseData.status == TransactionStatusEnum.EXPIRED || responseData.status == TransactionStatusEnum.SUCCESS) {
                    isSessionLoaded = true
                    isBoxPayAnimationLoading.value = false
                    _state.value = UiState.Success(sessionData.toUiModel())
                    return@launch
                }

                // Set user data
                val countryDetails = getPhoneNumberCodeAndCountryName(
                    shopper.deliveryAddress?.countryCode ?: "IN"
                )
                UserDataHandler.setUserPhoneAndCountryData(
                    phoneCode           = countryDetails?.isdCode,
                    countryName         = countryDetails?.fullName,
                    completePhoneNumber = shopper.phoneNumber,
                    countryCode         = shopper.deliveryAddress?.countryCode
                )
                UserDataHandler.setUniqueRef(shopper.uniqueReference)

                val instruments: List<SelectedPaymentMethod>?

                supervisorScope {
                    val instrumentsDeferred = async {
                        if (!checkoutDetails.shopperToken.isNullOrBlank())
                            repo.fetchRecommendedInstruments()
                        else null
                    }

                    // ✅ Surcharge runs and completes here before moving on
                    val surchargeJob = async {
                        fetchSurchargeAndApply(
                            amount       = responseData.totalAmount,
                            currencyCode = responseData.currencyCode
                        )
                    }

                    instruments = runCatching { instrumentsDeferred.await() }
                        .getOrNull()
                        ?.let { (it as? ApiResponse.Success)?.data?.toUiModel() }

                    // ✅ Explicitly wait for surcharge to finish
                    surchargeJob.await()
                }

                if (!instruments.isNullOrEmpty()) {
                    recommendedList.value      = instruments.filter { it.type.lowercase() == "upi" }.take(2)
                    upiRecommendedList.value   = instruments.filter { it.type.lowercase() == "upi" }
                    cardsRecommendedList.value = instruments.filter { it.type.lowercase() == "card" }
                }

                isSessionLoaded = true
                isBoxPayAnimationLoading.value = false
                _state.value = UiState.Success(sessionData.toUiModel())

                callUiAnalytics(
                    event = AnalyticsEvents.CHECKOUT_LOADED.value,
                    screenName = "MainScreenViewModel",
                    message = "Checkout loaded successfully in function load session"
                )

            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: checkoutDetails.errorMessage)
                callUiAnalytics(
                    event = AnalyticsEvents.SDK_CRASH.value,
                    screenName = "MainScreenViewModel",
                    message = "Error in loading checkout $e"
                )
            }
        }
    }

    // ── Surcharge function — suspend so loadSession waits for it ──────────────────
    private suspend fun fetchSurchargeAndApply(
        amount: Double,
        currencyCode: String
    ) {
        val surchargeResult = runCatching {
            repo.getSurcharge(amount, currencyCode)
        }.getOrNull()
            ?.let { (it as? ApiResponse.Success)?.data?.toUiModel() }

        surchargeResult?.let {
            CheckoutDetailsHandler.setSurchargeDetails(it)
        }
    }

    fun getPhoneNumberCodeAndCountryName(countryCode: String): CountryDetailsModel? {
        return countryData[countryCode]
    }

    // In ViewModel
//    fun startSessionCountdown(sessionExpiryTimestamp: String) {
//        if (sessionExpiryTimestamp.isEmpty()) return
//        if (countdownJob?.isActive == true) return
//
//        countdownJob?.cancel()
//        countdownJob = viewModelScope.launch {
//            val expiryMillis = parseIso8601ToMillis(sessionExpiryTimestamp)  // ← no Instant ✅
//
//            while (isActive) {
//                val nowMillis = currentTimeMillis()
//                val timeDiff  = expiryMillis - nowMillis
//                sessionSeconds.value = timeDiff /1000L
//
//                if (timeDiff <= 0) {
//                    callCheckoutSessionExpireModal(transactionId = CheckoutDetailsHandler.checkoutDetails.transactionId)
//                    SDKJobHandler.cancelAll()
//                    break
//                }
//                delay(1000L)
//            }
//        }
//        SDKJobHandler.register(countdownJob!!)  // ← register ✅
//    }


    override fun onCleared() {
        super.onCleared()
        lifecycleObserver.stop()
        countdownJob?.cancel()
    }

    fun postUpiCollectRequest(shopperVpa : String, type : String, instrumentRef : String? = null, saveInstrument : Boolean? = null) {
        viewModelScope.launch {
            isBoxPayAnimationLoading.value = true
            upiId.value = shopperVpa
            callUiAnalytics(
                event = AnalyticsEvents.PAYMENT_INITIATED.value,
                screenName = "MainScreenViewModel",
                message = "Payment initiated though collect method and function is postUpiCollectRequest"
            )
            val response = repo.postUpiCollectRequest(type = type,shopperVpa = shopperVpa, instrumentRef = instrumentRef , saveInstrument = saveInstrument)
            handlePaymentResponse(
                response = response,
                onSetPaymentUrl = {
                    setWebViewUrl.value = it
                    setWebViewScreen(true)
                },
                onSetPaymentHtml = {
                    setWebViewHtml.value = it
                    setWebViewScreen(true)
                },
                onNavigateToTimer = {
                    proceedToTimer.value = true
                },
                onOpenQr = {_,_ ->
                    // no operation
                },
                onOpenUpiIntent = {_ ->
                    // no operation
                },
                errorMessage = CheckoutDetailsHandler.checkoutDetails.errorMessage,
                setIsBoxPayAnimationVisible = {
                    stopFetchStatusPolling()
                    isBoxPayAnimationLoading.value = it
                }
            )
        }
    }

    fun postUpiIntentRequest(
        selectedIntent : String,
        type : String,
    ) {
        viewModelScope.launch {
            isBoxPayAnimationLoading.value = true
            callUiAnalytics(
                event = AnalyticsEvents.PAYMENT_INITIATED.value,
                screenName = "MainScreenViewModel",
                message = "Payment initiated though intent method and function is postUpiIntentRequest"
            )
            val response = repo.postUpiIntentRequest(type = type, upiApp = selectedIntent )
            handlePaymentResponse(
                response = response,
                onSetPaymentUrl = {
                    setWebViewUrl.value = it
                    setWebViewScreen(true)
                },
                onSetPaymentHtml = {
                    setWebViewHtml.value = it
                    setWebViewScreen(true)
                },
                onNavigateToTimer = {
                    // no operations
                },
                onOpenQr = {_, _ ->
                    // no operations
                },
                onOpenUpiIntent = {url ->
                    val decodedUrl = decodeBase64Url(url)
                    if (decodedUrl.isNotEmpty()) {
                        lifecycleObserver.start()
                        upiIntentUrl.value = decodedUrl
                    }
                },
                errorMessage = CheckoutDetailsHandler.checkoutDetails.errorMessage,
                setIsBoxPayAnimationVisible = {
                    stopFetchStatusPolling()
                    isBoxPayAnimationLoading.value = it
                }
            )
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decodeBase64Url(base64String: String): String {
        try {
            val decodedBytes = Base64.decode(base64String)
            return decodedBytes.decodeToString()
        } catch (error : Exception) {
            CheckoutDetailsHandler.setSessionFailed()
            return error.toString()
        }
    }

    fun callFetchStatus(inquiryResult : String) {
        viewModelScope.launch {
            CheckoutDetailsHandler.setInquiryToken(inquiryResult)
            isBoxPayAnimationLoading.value = true
            val response = otherPaymentMethodRepo.fetchStatus()
            handleFetchStatus(
                response = response,
                setIsBoxPayAnimationVisible = {
                    isBoxPayAnimationLoading.value = it
                }
            )
        }
    }

    fun callUpiCollectFetchStatue(inquiryResult : String) {
        viewModelScope.launch {
            CheckoutDetailsHandler.setInquiryToken(inquiryResult)
            val response = otherPaymentMethodRepo.fetchStatus()
            handleUpiCollectFetchStatus(
                response = response,
                setIsBoxPayAnimationVisible = {
                    stopFetchStatusPolling()
                    isBoxPayAnimationLoading.value = it
                }
            )
        }
    }

    fun startFetchStatusPolling(inquiryResult: String) {
        fetchStatusJob?.cancel()
        fetchStatusJob = viewModelScope.launch {
            while (isActive) {
                callUpiCollectFetchStatue(inquiryResult)
                delay(4000L)
            }
        }
    }

    fun stopFetchStatusPolling() {
        fetchStatusJob?.cancel()
        fetchStatusJob = null
    }

    fun postSavedCardRequest(instrumentRef : String, isSICheckboxChecked : Boolean) {
        viewModelScope.launch {
            isBoxPayAnimationLoading.value = true
            callUiAnalytics(
                event = AnalyticsEvents.PAYMENT_INITIATED.value,
                screenName = "MainScreenViewModel",
                message = "Payment initiated though saved card method and function is postSavedCardRequest"
            )
            val response = repo.postSavedCardRequest(instrumentRef = instrumentRef, isSICheckboxChecked = isSICheckboxChecked )
            handlePaymentResponse(
                response = response,
                onSetPaymentUrl = {
                    setWebViewUrl.value = it
                    setWebViewScreen(true)
                },
                onSetPaymentHtml = {
                    setWebViewHtml.value = it
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
                    stopFetchStatusPolling()
                    isBoxPayAnimationLoading.value = it
                }
            )
        }
    }
    fun setWebViewScreen(boolean: Boolean) {
        showWebViewScreen.value = boolean
        CheckoutDetailsHandler.setIsWebViewVisible(boolean)
    }

    fun onClickDeleteSavedCard(id : String) {
        viewModelScope.launch {
            isBoxPayAnimationLoading.value = true
            callUiAnalytics(
                event = AnalyticsEvents.PAYMENT_INSTRUMENT_PROVIDED.value,
                screenName = "MainScreenViewModel",
                message = "Card deleted in function onClickDeleteSavedCard"
            )
            val response = repo.deleteSavedCard(id)
            when(response) {
                is ApiResponse.Success -> {
                    isBoxPayAnimationLoading.value = false
                    cardsRecommendedList.value = cardsRecommendedList.value.filter { it.id != id }
                }
                is ApiResponse.Error -> {
                    isBoxPayAnimationLoading.value = false
                    isToastVisible.value = true
                }
                else -> {}
            }
        }
    }

//    fun stopSessionCountDown() {
//        countdownJob?.cancel()
//        countdownJob = null
//    }

    fun callUiAnalytics(
        event : String,
        screenName : String,
        message : String
    ) {
        viewModelScope.launch {
            analyticsRepo.callUiAnalytics(event, screenName, message)
        }
    }

    fun postUPIQrRequest(
        type : String
    ) {
        viewModelScope.launch {
            stopFetchStatusPolling()
            isBoxPayAnimationLoading.value = true
            callUiAnalytics(
                event = AnalyticsEvents.PAYMENT_INITIATED.value,
                screenName = "MainScreenViewModel",
                message = "Payment initiated though QR method and function is postUPIQrRequest"
            )
            val response = repo.postUPIQrRequest(type = type)
            handlePaymentResponse(
                response = response,
                onSetPaymentUrl = {
                    setWebViewUrl.value = it
                    setWebViewScreen(true)
                },
                onSetPaymentHtml = {
                    setWebViewHtml.value = it
                    setWebViewScreen(true)
                },
                onNavigateToTimer = {
                    // no operations
                },
                onOpenQr = {image, expiry ->
                    // no operations
                    qrImage.value = image
                    qrTimer.value = expiry
                    startFetchStatusPolling("")
                    isBoxPayAnimationLoading.value = false
                },
                onOpenUpiIntent = {url ->
                    val decodedUrl = decodeBase64Url(url)
                    if (decodedUrl.isNotEmpty()) {
                        lifecycleObserver.start()
                        upiIntentUrl.value = decodedUrl
                    }
                },
                errorMessage = CheckoutDetailsHandler.checkoutDetails.errorMessage,
                setIsBoxPayAnimationVisible = {
                    isBoxPayAnimationLoading.value = it
                }
            )
        }
    }

    fun removeQRFromView() {
        stopFetchStatusPolling()
        qrTimer.value = 0
        qrImage.value = ""
    }

    fun onWebViewDismissed() {
        // Stop any webview-related polling or pending state
        CheckoutDetailsHandler.setIsWebViewVisible(false)
    }

    fun onUPITimerBottomSheetDismissed() {
        proceedToTimer.value = false
        upiId.value = ""
    }

    fun fetchCardDetails(cardNumber : String, isTestEnv: Boolean) {
        viewModelScope.launch {
            when(val response = cardRepo.getCardDetails(cardNumber)) {
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

    fun postCardRequest(isSICheckBoxClicked : Boolean) {
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
            isBoxPayAnimationLoading.value = true
            val response = cardRepo.postCardDetails(
                type = "card/plain",
                cardNumber = cardNumberText.value,
                cardName = cardHolderNameText.value,
                expiry = formatExpiryForApi(cardExpiryText.value),
                cvv = cardCvvText.value,
                nickName = cardNickNameText.value,
                isSaveInstrumentCheckboxClicked = isSavedCardCheckBoxClicked.value,
                isSICheckboxClicked = !isSICheckBoxClicked
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
                setIsBoxPayAnimationVisible = {isBoxPayAnimationLoading.value = it},
                errorMessage = CheckoutDetailsHandler.checkoutDetails.errorMessage
            )
        }
    }

    fun formatExpiryForApi(expiry: String): String {
        if (expiry.length != 4) return ""
        val month = expiry.take(2)       // "05"
        val year  = "20${expiry.drop(2)}" // "30" → "2030"
        return "$year-$month"            // "2030-05"
    }

    fun loadBanksList(type : String) {
        viewModelScope.launch {
            val checkoutDetails = CheckoutDetailsHandler.checkoutDetails
            _uiState.value = when (val response = otherPaymentMethodRepo.getPaymentMethods(
                amount = if (checkoutDetails.appliedOfferId.isNullOrBlank()) checkoutDetails.amount else null,
                offerId = checkoutDetails.appliedOfferId
            )) {
                is ApiResponse.Error -> {
                    UiState.Error(response.message)
                }
                ApiResponse.Loading -> {
                    UiState.Loading
                }
                is ApiResponse.Success -> {
                    allBanks.value = response.data.toUiModel(type)
                    UiState.Success(response.data.toUiModel(type))
                }
            }
        }
    }

    fun onSearch(text: String) {
        netBankingSearchQuery.value = text

        val filtered = if (text.isBlank()) {
            allBanks.value
        } else {
            allBanks.value.filter {
                it.displayName.trim()
                    .contains(text.trim(), ignoreCase = true)
            }
        }

        _uiState.value = UiState.Success(filtered)
    }

    fun postOtherRequest(instrumentValue: String, type: String) {
        viewModelScope.launch {
            callUiAnalytics(
                event = AnalyticsEvents.PAYMENT_INITIATED.value,
                screenName = "$type viewmodel in elements",
                message = "payment initiated"
            )
            isBoxPayAnimationLoading.value = true
            val response = otherPaymentMethodRepo.initiatePayment(
                instrumentDetails = instrumentValue
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
                setIsBoxPayAnimationVisible = {isBoxPayAnimationLoading.value = it},
                errorMessage = CheckoutDetailsHandler.checkoutDetails.errorMessage
            )
        }
    }


    // ─────────────────────────────────────────────────────────────────────────────
// Merchant-button support (Elements embedded mode)
// Tabs report what's currently selected; isPayable/submit derive from it.
// ─────────────────────────────────────────────────────────────────────────────

    /** What the user has currently chosen to pay with, per active tab. */
    sealed interface PaySelection {
        data object None : PaySelection
        data class UpiCollect(val vpa: String) : PaySelection
        data class UpiIntent(val selectedIntent: String) : PaySelection
        data object Card : PaySelection                                  // validity read from cardValid
        data class Instrument(val value: String, val type: String) : PaySelection  // bank/wallet/bnpl
        data class SavedUpi(val instrumentRef: String, val vpa: String) : PaySelection
        data class SavedCard(val instrumentRef: String) : PaySelection
    }

    private val _paySelection = MutableStateFlow<PaySelection>(PaySelection.None)

    /** Drives the merchant's button enabled-state. */
    val isPayable: StateFlow<Boolean> =
        combine(_paySelection, cardValidFlow()) { selection, cardOk ->
            when (selection) {
                is PaySelection.None         -> false
                is PaySelection.UpiCollect   -> selection.vpa.trim().isNotEmpty() && upiRegex.matches(selection.vpa)
                is PaySelection.UpiIntent -> selection.selectedIntent.isNotBlank()
                is PaySelection.Card         -> cardOk
                is PaySelection.Instrument   -> selection.value.isNotBlank()
                is PaySelection.SavedUpi     -> selection.instrumentRef.isNotBlank()
                is PaySelection.SavedCard    -> selection.instrumentRef.isNotBlank()
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Called by each tab whenever its selection/validity changes. */
    fun setPaySelection(selection: PaySelection) {
        _paySelection.value = selection
    }

    /** Triggered by the merchant's pay button via the controller. */
    fun submitSelectedInstrument() {
        when (val selection = _paySelection.value) {
            is PaySelection.None -> {
                // nothing selected — no-op (button should be disabled anyway)
            }
            is PaySelection.UpiIntent ->
                postUpiIntentRequest(selectedIntent = selection.selectedIntent, type = "upi/intent")
            is PaySelection.UpiCollect ->
                postUpiCollectRequest(shopperVpa = selection.vpa, type = "upi/collect")
            is PaySelection.Card ->
                postCardRequest(isSICheckBoxClicked = isSavedCardCheckBoxClicked.value)
            is PaySelection.Instrument ->
                postOtherRequest(instrumentValue = selection.value, type = selection.type)
            is PaySelection.SavedUpi ->
                postUpiCollectRequest(
                    shopperVpa = selection.vpa,
                    type = "upi/collect",
                    instrumentRef = selection.instrumentRef
                )
            is PaySelection.SavedCard ->
                postSavedCardRequest(
                    instrumentRef = selection.instrumentRef,
                    isSICheckboxChecked = isSavedCardCheckBoxClicked.value
                )
        }
    }
}