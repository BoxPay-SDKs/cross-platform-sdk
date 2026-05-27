package com.crossplatform.sdk.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.handler.SDKJobHandler
import com.crossplatform.sdk.data.handler.UserDataHandler
import com.crossplatform.sdk.data.model.AnalyticsEvents
import com.crossplatform.sdk.domain.mapper.toUiModel
import com.crossplatform.sdk.domain.model.AppLifecycleState
import com.crossplatform.sdk.domain.model.CountryDetailsModel
import com.crossplatform.sdk.domain.model.MainScreenModel
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import com.crossplatform.sdk.domain.repo.MainScreenRepo
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
    private val analyticsRepo : CallUIAnalyticsRepo
): ViewModel() {

    private val _state = MutableStateFlow<UiState<MainScreenModel>>(UiState.Loading)
    val state: StateFlow<UiState<MainScreenModel>> = _state
    private var isSessionLoaded = false

    val isToastVisible = MutableStateFlow(false)
    private var countryData: Map<String, CountryDetailsModel> = emptyMap()
    private var countdownJob: Job? = null
    val isBoxPayAnimationLoading = MutableStateFlow(false)

    val isLoadingSession = mutableStateOf(true)
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

    private val lifecycleObserver = AppLifecycleObserver { state ->
        if (state == AppLifecycleState.Foreground && isUpiOpening.value) {
            callFetchStatus("")
        }
    }


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
                val shopper = sessionData.paymentDetails.shopper
                val money = sessionData.paymentDetails.money

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

                // ✅ supervisorScope isolates failures between the two async blocks
                supervisorScope {
                    val instrumentsDeferred = async {
                        if (!checkoutDetails.shopperToken.isNullOrBlank())
                            repo.fetchRecommendedInstruments()
                        else null
                    }
                    val surchargeDeferred = async {
                        repo.getSurcharge(money.amount, money.currencyCode)
                    }

                    // ✅ Catch individually so one failure doesn't block the other
                    val instruments = runCatching { instrumentsDeferred.await() }
                        .getOrNull()
                        ?.let { (it as? ApiResponse.Success)?.data?.toUiModel() }

                    if (!instruments.isNullOrEmpty()) {
                        recommendedList.value      = instruments.filter { it.type.lowercase() == "upi" }.take(2)
                        upiRecommendedList.value   = instruments.filter { it.type.lowercase() == "upi" }
                        cardsRecommendedList.value = instruments.filter { it.type.lowercase() == "card" }
                    }

                    runCatching { surchargeDeferred.await() }
                        .getOrNull()
                        ?.let { (it as? ApiResponse.Success)?.data?.toUiModel() }
                        ?.let { CheckoutDetailsHandler.setSurchargeDetails(it) }
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
                onOpenQr = {_ ->
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
                onOpenQr = {_ ->
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
            val response = repo.fetchStatus()
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
            isBoxPayAnimationLoading.value = true
            val response = repo.fetchStatus()
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
                onOpenQr = {_ ->
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
                event = AnalyticsEvents.ADDRESS_UPDATED.value,
                screenName = "MainScreenViewModel",
                message = "Address updated in function onClickDeleteSavedCard"
            )
            val response = repo.deleteSavedCard(id)
            when(response) {
                is ApiResponse.Success -> {
                    isBoxPayAnimationLoading.value = true
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
}