package com.crossplatform.sdk.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AnalyticsEvents
import com.crossplatform.sdk.domain.model.AppLifecycleState
import com.crossplatform.sdk.domain.model.PayNowUiState
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import com.crossplatform.sdk.domain.repo.FetchStatusRepo
import com.crossplatform.sdk.domain.repo.OtherPaymentMethodRepo
import com.crossplatform.sdk.presentation.AppLifecycleObserver
import com.crossplatform.sdk.presentation.getStatus
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

    private val _qrState = MutableStateFlow<PayNowUiState>(PayNowUiState.Idle)
    val qrState : StateFlow<PayNowUiState> get() = _qrState
    val isQRFetching = MutableStateFlow(false)
    private var fetchStatusJob: Job? = null

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

    fun getPayNowQR(instrumentType : String) {
        viewModelScope.launch {
            callUiAnalytics(
                event = AnalyticsEvents.PAYMENT_INITIATED.value,
                screenName = "PayNowScreenViewModel",
                message = "payment initiated"
            )
            when (val response = repo.initiatePayment(
                instrumentDetails = instrumentType,
                paymentType = "",
                token = ""
            )) {
                is ApiResponse.Error ->  {
                    isQRFetching.value = false
                    CheckoutDetailsHandler.setErrorMessage(CheckoutDetailsHandler.checkoutDetails.errorMessage)
                    CheckoutDetailsHandler.setSessionFailed()
                }
                ApiResponse.Loading -> {
                    isQRFetching.value = true
                }
                is ApiResponse.Success -> {
                    val apiData = response.data
                    val status = getStatus(apiData.status.status)
                    val transactionId = apiData.transactionId

                    CheckoutDetailsHandler.setStatusAndTransID(
                        status = status.name,
                        transactionId = transactionId
                    )
                    isQRFetching.value = false
                    _qrState.value = PayNowUiState.Ready(response.data.actions?.get(0)?.content ?: "", response.data.actions?.get(0)?.expirySec ?: 300)
                    startFetchStatusPolling()
                }
            }
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
                callUpiCollectFetchStatue()
                delay(4000L)
            }
        }
    }

    fun callUpiCollectFetchStatue() {
        viewModelScope.launch {
            val response = fetchStatusRepo.fetchStatus()
            handleUpiCollectFetchStatus(
                response = response,
                setIsBoxPayAnimationVisible = {
                    stopFetchStatusPolling()
                }
            )
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
}