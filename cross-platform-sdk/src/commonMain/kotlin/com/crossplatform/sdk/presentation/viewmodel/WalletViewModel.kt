package com.crossplatform.sdk.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.AnalyticsEvents
import com.crossplatform.sdk.domain.mapper.toUiModel
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import com.crossplatform.sdk.domain.repo.FetchStatusRepo
import com.crossplatform.sdk.domain.repo.OtherPaymentMethodRepo
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.sharedContext.handleFetchStatus
import com.crossplatform.sdk.presentation.sharedContext.handlePaymentResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WalletViewModel (
    private val repo : OtherPaymentMethodRepo,
    private val analyticsRepo : CallUIAnalyticsRepo,
    private val fetchStatusRepo: FetchStatusRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<SelectedPaymentMethod>>>(UiState.Loading)
    val uiState : StateFlow<UiState<List<SelectedPaymentMethod>>> get() = _uiState
    private var allWallets = mutableStateOf<List<SelectedPaymentMethod>>(emptyList())
    val searchQuery = mutableStateOf("")

    val isBoxPayAnimationVisible = MutableStateFlow(false)
    val showWebview = MutableStateFlow(false)
    val url = mutableStateOf<String?>(null)
    val htmlString = mutableStateOf<String?>(null)

    init {
        loadWalletList()
    }

    fun loadWalletList() {
        viewModelScope.launch {
            val checkoutDetails = CheckoutDetailsHandler.checkoutDetails
            _uiState.value = when (val response = repo.getPaymentMethods(
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
                    allWallets.value = response.data.toUiModel("wallet")
                    UiState.Success(response.data.toUiModel("wallet"))
                }
            }
        }
    }

    fun onSearch(text: String) {
        searchQuery.value = text

        val filtered = if (text.isBlank()) {
            allWallets.value
        } else {
            allWallets.value.filter {
                it.displayName.trim()
                    .contains(text.trim(), ignoreCase = true)
            }
        }

        _uiState.value = UiState.Success(filtered)
    }

    fun postWalletRequest(instrumentValue: String) {
        viewModelScope.launch {
            callUiAnalytics(
                event = AnalyticsEvents.PAYMENT_INITIATED.value,
                screenName = "WalletViewModel",
                message = "Payment initiated"
            )
            isBoxPayAnimationVisible.value = true
            val response = repo.initiatePayment(
                instrumentDetails = instrumentValue,
                paymentType = "wallet",
                token = CheckoutDetailsHandler.checkoutDetails.token
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