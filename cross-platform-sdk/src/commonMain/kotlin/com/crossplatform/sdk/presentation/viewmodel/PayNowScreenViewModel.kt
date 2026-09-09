package com.crossplatform.sdk.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AnalyticsEvents
import com.crossplatform.sdk.domain.model.AppLifecycleState
import com.crossplatform.sdk.domain.model.PayNowUiState
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import com.crossplatform.sdk.domain.repo.FetchStatusRepo
import com.crossplatform.sdk.domain.repo.OtherPaymentMethodRepo
import com.crossplatform.sdk.presentation.AppLifecycleObserver
import com.crossplatform.sdk.presentation.sharedContext.handlePaymentResponse
import com.crossplatform.sdk.presentation.sharedContext.handleUpiCollectFetchStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class PayNowScreenViewModel(
    private val repo: OtherPaymentMethodRepo,
    private val analyticsRepo : CallUIAnalyticsRepo,
    private val fetchStatusRepo: FetchStatusRepo
) : ViewModel()  {

    private val _qrState = MutableStateFlow<PayNowUiState?>(null)
    val qrState : StateFlow<PayNowUiState?> get() = _qrState
    val isQRFetching = MutableStateFlow(true)
    private var fetchStatusJob: Job? = null
    val showWebview = MutableStateFlow(false)
    val url = mutableStateOf<String?>(null)
    val htmlString = mutableStateOf<String?>(null)

    private var isPollingIntended = false

    val lifecycleObserver = AppLifecycleObserver { state ->
        when (state) {
            AppLifecycleState.Foreground -> {
                if (isPollingIntended) resumePolling()
            }
            AppLifecycleState.Background -> {
                pausePolling()
            }
            else -> {
                stopFetchStatusPolling()
            }
        }
    }

    fun getPayNowQR(instrumentType : String, surcharge : List<String>?) {
        isQRFetching.value = true
        viewModelScope.launch {
            callUiAnalytics(
                event = AnalyticsEvents.PAYMENT_INITIATED.value,
                screenName = "PayNowScreenViewModel",
                message = "payment initiated"
            )
            val response = repo.initiatePayment(
                instrumentDetails = instrumentType,
                paymentType = "",
                token = "",
                surcharge
            )
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
//                    proceedToTimer.value = true
                },
                onOpenQr = {content,expirySec ->
                    isQRFetching.value = false
                    _qrState.value = PayNowUiState.Ready(content, expirySec)
                    startFetchStatusPolling()
                },
                onOpenUpiIntent = {_ ->
                    // no operation
                },
                errorMessage = CheckoutDetailsHandler.checkoutDetails.errorMessage,
                setIsBoxPayAnimationVisible = {
                    stopFetchStatusPolling()
                    isQRFetching.value = it
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        lifecycleObserver.stop()
        fetchStatusJob?.cancel()
    }

    fun markExpired() {
        _qrState.value = PayNowUiState.Expired
    }

    fun startFetchStatusPolling() {
        isPollingIntended = true
        fetchStatusJob?.cancel()
        fetchStatusJob = viewModelScope.launch {
            while (isActive) {
                callFetchStatus("")
                delay(4000L)
            }
        }
    }

    fun pausePolling() {
        fetchStatusJob?.cancel()
        fetchStatusJob = null
    }

    // called from ON_START - resume only if we were mid-flow, and check immediately
    fun resumePolling() {
        if (isPollingIntended && fetchStatusJob == null && _qrState.value is PayNowUiState.Ready) {
            startFetchStatusPolling()
        }
    }

    fun stopFetchStatusPolling() {
        isPollingIntended = false
        fetchStatusJob?.cancel()
        fetchStatusJob = null
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

    fun callFetchStatus(inquiryResult : String) {
        viewModelScope.launch {
            CheckoutDetailsHandler.setInquiryToken(inquiryResult)
            val response = fetchStatusRepo.fetchStatus()
            handleUpiCollectFetchStatus(
                response = response,
                setIsBoxPayAnimationVisible = {
                    isQRFetching.value = it
                    stopFetchStatusPolling()
                                              }
            )
        }
    }

    fun setWebViewScreen(boolean: Boolean) {
        showWebview.value = boolean
        CheckoutDetailsHandler.setIsWebViewVisible(boolean)
    }

    fun setExpiredQRState() {
        _qrState.value = PayNowUiState.Expired
    }

    fun autoRetryInitiatePayment() {
        viewModelScope.launch {
            isQRFetching.value = true
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
                    isQRFetching.value = it
                }
            )
        }
    }

    fun resolveSurchargeList(
        selectedMethod: String,
        selectedNetwork: String = ""
    ): List<String>? {
        val surcharges = CheckoutDetailsHandler.surchargeDetailsFlow.value
        val filtered = surcharges.filter { item ->
            val applicable = item.applicableOn.lowercase().trim()

            val methodMatches = applicable.isNotEmpty() &&
                    applicable == selectedMethod.lowercase().trim()

            val networkMatches = item.network.isBlank() ||
                    item.network.replace(" ", "").equals(selectedNetwork.replace(" ", ""), true)

            (applicable.isBlank() || methodMatches) && networkMatches
        }
        return filtered.map { it.surchargeCode }.ifEmpty { null }
    }
}