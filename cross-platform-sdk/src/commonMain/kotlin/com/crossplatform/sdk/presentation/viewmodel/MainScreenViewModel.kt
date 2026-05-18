package com.crossplatform.sdk.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.handler.SDKJobHandler
import com.crossplatform.sdk.data.handler.UserDataHandler
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.domain.mapper.toUiModel
import com.crossplatform.sdk.domain.model.AppLifecycleState
import com.crossplatform.sdk.domain.model.CountryDetailsModel
import com.crossplatform.sdk.domain.model.MainScreenModel
import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import com.crossplatform.sdk.domain.repo.MainScreenRepo
import com.crossplatform.sdk.presentation.AppLifecycleObserver
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.currentTimeMillis
import com.crossplatform.sdk.presentation.loadCountryData
import com.crossplatform.sdk.presentation.parseIso8601ToMillis
import com.crossplatform.sdk.presentation.sharedContext.handleFetchStatus
import com.crossplatform.sdk.presentation.sharedContext.handlePaymentResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class MainScreenViewModel(
    private val repo: MainScreenRepo
): ViewModel() {

    private val _state = MutableStateFlow<UiState<MainScreenModel>>(UiState.Loading)
    val state: StateFlow<UiState<MainScreenModel>> = _state
    private var isSessionLoaded = false  // ← fla
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

    private val lifecycleObserver = AppLifecycleObserver { state ->
        if (state == AppLifecycleState.Foreground && isUpiOpening.value) {
            callFetchStatus()
        }
    }


    init {
        loadSession()
    }

    fun loadSession() {
        if (isSessionLoaded) return
        val checkoutDetails = CheckoutDetailsHandler.checkoutDetails
        println("====loadsession called ${checkoutDetails.token}")
        if (checkoutDetails.token.isEmpty()) {
            _state.value = UiState.Error("Token is empty")
            return
        }

        viewModelScope.launch {
            try {
                countryData = loadCountryData()
                println("======countrydata $countryData")
                _state.value = when (val response = repo.getSessionDetails()) {
                    is ApiResponse.Error -> {
                        UiState.Error(response.errorBody ?: response.message)
                    }
                    ApiResponse.Loading -> {
                        UiState.Loading
                    }
                    is ApiResponse.Success -> {
                        val countryDetails = getPhoneNumberCodeAndCountryName(response.data.paymentDetails.shopper.deliveryAddress?.countryCode ?: "IN")
                        UserDataHandler.setUserPhoneAndCountryData(
                            phoneCode = countryDetails?.isdCode,
                            countryName = countryDetails?.fullName,
                            completePhoneNumber = response.data.paymentDetails.shopper.phoneNumber,
                            countryCode = response.data.paymentDetails.shopper.deliveryAddress?.countryCode
                        )
                        isSessionLoaded = true
                        UiState.Success(response.data.toUiModel())
                    }
                }
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: checkoutDetails.errorMessage)
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
        CheckoutDetailsHandler.setStatusAndTransID(
            status = TransactionStatusEnum.EXPIRED.name,
            transactionId = transactionId
        )
        CheckoutDetailsHandler.setSessionExpired()
    }

    fun callCheckoutSessionSuccessModal(
        transactionId : String
    ) {
        CheckoutDetailsHandler.setStatusAndTransID(
            status = TransactionStatusEnum.SUCCESS.name,
            transactionId = transactionId
        )
        CheckoutDetailsHandler.setSessionSuccess()
    }

    fun postUpiCollectRequest(shopperVpa : String, type : String, instrumentRef : String? = null) {
        viewModelScope.launch {
            isBoxPayAnimationLoading.value = true
            upiId.value = shopperVpa
            val response = repo.postUpiCollectRequest(type = type,shopperVpa = shopperVpa, instrumentRef = instrumentRef )
            handlePaymentResponse(
                response = response,
                onSetPaymentUrl = { setWebViewUrl.value = it },
                onSetPaymentHtml = { setWebViewHtml.value = it },
                onNavigateToTimer = { proceedToTimer.value = true },
                onOpenQr = {_ ->
                    // no operation
                },
                onOpenUpiIntent = {_ ->
                    // no operation
                },
                errorMessage = CheckoutDetailsHandler.checkoutDetails.errorMessage,
                setIsBoxPayAnimationVisible = {isBoxPayAnimationLoading.value = it}
            )
        }
    }

    fun postUpiIntentRequest(
        selectedIntent : String,
        type : String,
    ) {
        viewModelScope.launch {
            isBoxPayAnimationLoading.value = true
            val response = repo.postUpiIntentRequest(type = type, upiApp = selectedIntent )
            handlePaymentResponse(
                response = response,
                onSetPaymentUrl = { setWebViewUrl.value = it },
                onSetPaymentHtml = { setWebViewHtml.value = it },
                onNavigateToTimer = { proceedToTimer.value = true },
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
                setIsBoxPayAnimationVisible = {isBoxPayAnimationLoading.value = it}
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

    fun callFetchStatus() {
        viewModelScope.launch {
            isBoxPayAnimationLoading.value = true
            val response = repo.fetchStatus()
            handleFetchStatus(
                response = response,
                setIsBoxPayAnimationVisible = {isBoxPayAnimationLoading.value = it}
            )
        }
    }
}