package com.crossplatform.sdk.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.handler.SDKJobHandler
import com.crossplatform.sdk.data.handler.UserDataHandler
import com.crossplatform.sdk.data.model.AnalyticsEvents
import com.crossplatform.sdk.domain.model.OfferItem
import com.crossplatform.sdk.domain.mapper.toUiModel
import com.crossplatform.sdk.domain.model.AppLifecycleState
import com.crossplatform.sdk.domain.model.CountryDetailsModel
import com.crossplatform.sdk.domain.model.MainScreenModel
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import com.crossplatform.sdk.domain.repo.InstantOfferRepo
import com.crossplatform.sdk.domain.repo.MainScreenRepo
import com.crossplatform.sdk.domain.repo.OtherPaymentMethodRepo
import com.crossplatform.sdk.presentation.AppLifecycleObserver
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.currentTimeMillis
import com.crossplatform.sdk.presentation.loadCountryData
import com.crossplatform.sdk.presentation.parseIso8601ToMillis
import com.crossplatform.sdk.presentation.sharedContext.handleFetchStatus
import com.crossplatform.sdk.presentation.sharedContext.handlePaymentResponse
import com.crossplatform.sdk.presentation.sharedContext.handleUpiCollectFetchStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class MainScreenViewModel(
    private val repo: MainScreenRepo,
    private val analyticsRepo : CallUIAnalyticsRepo,
    private val otherPaymentMethodRepo: OtherPaymentMethodRepo,
    private val instantOfferRepo: InstantOfferRepo
): ViewModel() {

    private val _state = MutableStateFlow<UiState<MainScreenModel>>(UiState.Loading)
    val state: StateFlow<UiState<MainScreenModel>> = _state
    private var isSessionLoaded = false

    val isToastVisible = MutableStateFlow(false)
    private var countryData: Map<String, CountryDetailsModel> = emptyMap()
    private var countdownJob: Job? = null
    val isBoxPayAnimationLoading = MutableStateFlow(false)

    val isLoadingSession = mutableStateOf(true)
    val isSwipeToPayVisible = mutableStateOf(false)
    val sessionSeconds = mutableStateOf(0L)
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
    val appliedOffers = mutableStateOf<List<OfferItem>>(emptyList())

    val qrTimer = mutableStateOf(0)
    val qrImage = mutableStateOf("")
    val revolutOrderToken = mutableStateOf("")
    val revolutReturnUrl = mutableStateOf("")

    val lifecycleObserver = AppLifecycleObserver { state ->
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
                val offers: List<OfferItem>?

                supervisorScope {
                    val instrumentsDeferred = async {
                        if (!checkoutDetails.shopperToken.isNullOrBlank())
                            repo.fetchRecommendedInstruments()
                        else null
                    }
                    val offersDeferred = async {
                        val hasApplicableOffer = sessionResponse.data.configs.paymentMethods
                            .any { !it.applicableOffer.isNullOrEmpty() }
                        if (hasApplicableOffer)
                            instantOfferRepo.getOffers(responseData.totalAmount, responseData.totalAmount)
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

                    offers = runCatching { offersDeferred.await() }
                        .getOrNull()
                        ?.let { (it as? ApiResponse.Success)?.data?.toUiModel() }
                        ?: emptyList()

                    // ✅ Explicitly wait for surcharge to finish
                    surchargeJob.await()
                }

                if (!instruments.isNullOrEmpty()) {
                    recommendedList.value      = instruments.filter { it.type.lowercase() == "upi" }.take(2)
                    upiRecommendedList.value   = instruments.filter { it.type.lowercase() == "upi" }
                    cardsRecommendedList.value = instruments.filter { it.type.lowercase() == "card" }
                }

                appliedOffers.value = offers ?: emptyList()

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
        when(val response = repo.getSurcharge(amount, currencyCode)) {
            is ApiResponse.Error -> {
                print("====error mrsshe ${response.errorBody}")
                CheckoutDetailsHandler.setSurchargeDetails(emptyList())
            }
            is ApiResponse.Success -> {
                val surcharges = response.data.toUiModel()

                // Sum surcharge fees that apply with no specific "applicableOn"
                val applicableSurcharge = surcharges
                    .filter { it.applicableOn.isEmpty() }
                    .sumOf { it.amount }

                val updatedAmount = amount + applicableSurcharge

                CheckoutDetailsHandler.setSurchargeDetails(surcharges)
                CheckoutDetailsHandler.setAmount(updatedAmount)
            }
            else -> {
                //  no op
            }
        }
    }

    fun getPhoneNumberCodeAndCountryName(countryCode: String): CountryDetailsModel? {
        return countryData[countryCode]
    }

    // In ViewModel
    fun startSessionCountdown(sessionExpiryTimestamp: String) {
        if (sessionExpiryTimestamp.isEmpty()) return
        if (countdownJob?.isActive == true) return

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            val expiryMillis = parseIso8601ToMillis(sessionExpiryTimestamp)  // ← no Instant ✅

            while (isActive) {
                val nowMillis = currentTimeMillis()
                val timeDiff  = expiryMillis - nowMillis
                sessionSeconds.value = timeDiff /1000L

                if (timeDiff <= 0) {
                    callCheckoutSessionExpireModal(transactionId = CheckoutDetailsHandler.checkoutDetails.transactionId)
                    SDKJobHandler.cancelAll()
                    break
                }
                delay(1000L)
            }
        }
        SDKJobHandler.register(countdownJob!!)  // ← register ✅
    }


    override fun onCleared() {
        super.onCleared()
        lifecycleObserver.stop()
        countdownJob?.cancel()
    }

    fun callCheckoutSessionExpireModal(
        transactionId : String
    ) {
        callUiAnalytics(
            event = AnalyticsEvents.PAYMENT_RESULT_SCREEN_DISPLAYED.value,
            screenName = "MainScreenViewModel",
            message = "Checkout session expired in function callCheckoutSessionExpireModal"
        )
        CheckoutDetailsHandler.setStatusAndTransID(
            status = TransactionStatusEnum.EXPIRED.name,
            transactionId = transactionId
        )
        CheckoutDetailsHandler.setSessionExpired()
    }

    fun callCheckoutSessionSuccessModal(
        transactionId : String
    ) {
        callUiAnalytics(
            event = AnalyticsEvents.PAYMENT_RESULT_SCREEN_DISPLAYED.value,
            screenName = "MainScreenViewModel",
            message = "Checkout session successful in function callCheckoutSessionSuccessModal"
        )
        CheckoutDetailsHandler.setStatusAndTransID(
            status = TransactionStatusEnum.SUCCESS.name,
            transactionId = transactionId
        )
        CheckoutDetailsHandler.setSessionSuccess()
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
            val decodedBytes = Base64.decode(base64String).decodeToString()
            return decodedBytes
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

    fun stopSessionCountDown() {
        countdownJob?.cancel()
        countdownJob = null
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

    fun applyOffer(selectedCode: String, amount: Double) {
        viewModelScope.launch {
            isBoxPayAnimationLoading.value = true
            when (val response = instantOfferRepo.applyOffer(listOf(selectedCode), amount)) {
                is ApiResponse.Success -> {
                    val appliedDiscount = response.data.evaluatedOffers?.get(0)?.appliedDiscountAmount ?: 0.0
                    val offerAmount = amount - appliedDiscount

                    CheckoutDetailsHandler.setAppliedOffer(
                        appliedOfferId = selectedCode,
                        amount = appliedDiscount
                    )
                    CheckoutDetailsHandler.setAmount(offerAmount)

                    // Run fetchPaymentMethods and fetchSurcharge in parallel
                    supervisorScope {
                        val paymentMethodsJob = async {
                            fetchPaymentMethods(offerAmount, selectedCode)
                        }
                        val surchargeJob = async {
                            fetchSurchargeAndApply(offerAmount, CheckoutDetailsHandler.checkoutDetails.currencyCode)
                        }
                        paymentMethodsJob.await()
                        surchargeJob.await()
                    }
                    isBoxPayAnimationLoading.value = false
                }
                is ApiResponse.Error -> {
                    isBoxPayAnimationLoading.value = false
                }
                else -> {}
            }
        }
    }

    fun removeOffer(discountAmount: Double, amount: Double) {
        viewModelScope.launch {
            isBoxPayAnimationLoading.value = true
            val originalAmount = amount + discountAmount
            CheckoutDetailsHandler.setAmount(originalAmount)
            CheckoutDetailsHandler.setAppliedOffer(appliedOfferId = "", amount = 0.0)

            // Run fetchPaymentMethods and fetchSurcharge in parallel
            supervisorScope {
                val paymentMethodsJob = async {
                    fetchPaymentMethods()
                }
                val surchargeJob = async {
                    fetchSurchargeAndApply(originalAmount, CheckoutDetailsHandler.checkoutDetails.currencyCode)
                }
                paymentMethodsJob.await()
                surchargeJob.await()
            }
            isBoxPayAnimationLoading.value = false
        }
    }

    fun fetchPaymentMethods(amount : Double? = null, offerId : String? = null) {
        viewModelScope.launch {
            when(val response = otherPaymentMethodRepo.getPaymentMethods(amount = amount, offerId = offerId)) {
                is ApiResponse.Error -> {
                    isBoxPayAnimationLoading.value = false
                }
                is ApiResponse.Success -> {
                    val currentState = _state.value as? UiState.Success ?: return@launch
                    var methodFlags = MainScreenModel.MethodFlags()
                    response.data.forEach { method ->
                        methodFlags = when (method.type) {
                            "Upi" -> {
                                when (method.brand) {
                                    "UpiIntent"  -> methodFlags.copy(isUPIIntentVisible = true, isUPIVisible = true)
                                    "UpiCollect" -> methodFlags.copy(isUPICollectVisible = true, isUPIVisible = true)
                                    "UpiQr"      -> methodFlags.copy(isUPIQRVisible = true, isUPIVisible = true)
                                    else         -> methodFlags
                                }
                            }
                            "UpiOneTimeMandate" -> {
                                when (method.brand) {
                                    "UpiIntentOtm"  -> methodFlags.copy(isUPIOtmIntentVisible = true, isUPIOtmVisible = true)
                                    "UpiCollectOtm" -> methodFlags.copy(isUPIOtmCollectVisible = true, isUPIOtmVisible = true)
                                    "UpiQrOtm"      -> methodFlags.copy(isUPIOtmQRVisible = true, isUPIOtmVisible = true)
                                    else            -> methodFlags
                                }
                            }
                            "Card"           -> methodFlags.copy(isCardsVisible      = true)
                            "Wallet"         -> methodFlags.copy(isWalletVisible     = true)
                            "NetBanking"     -> methodFlags.copy(isNetBankingVisible = true)
                            "Emi"            -> methodFlags.copy(isEMIVisible        = true)
                            "BuyNowPayLater" -> methodFlags.copy(isBNPLVisible       = true)
                            else             -> methodFlags
                        }
                    }

                    _state.value = currentState.copy(
                        data = currentState.data.copy(methodFlags = methodFlags)
                    )
                }
                else -> {
                    // no op
                }
            }
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

    fun onClickRevolutPay() {
        viewModelScope.launch {
            callUiAnalytics(
                event = AnalyticsEvents.PAYMENT_INITIATED.value,
                screenName = "WalletViewModel",
                message = "Payment initiated"
            )
            isBoxPayAnimationLoading.value = true
            val response = otherPaymentMethodRepo.initiatePayment(
                instrumentDetails = "wallet/revolutpay"
            )
            handlePaymentResponse(
                response = response,
                onSetPaymentHtml = {html ->
                    // no operation
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
                    // no operation
                },
                setIsBoxPayAnimationVisible = {isBoxPayAnimationLoading.value = it},
                errorMessage = CheckoutDetailsHandler.checkoutDetails.errorMessage,
                onRevolutPay = {orderToken, returnUrl ->
                    revolutOrderToken.value = orderToken
                    revolutReturnUrl.value = returnUrl
                }
            )
        }
    }

}