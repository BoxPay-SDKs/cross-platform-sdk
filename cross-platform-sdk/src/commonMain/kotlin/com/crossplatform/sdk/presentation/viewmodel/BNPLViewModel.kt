package com.crossplatform.sdk.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.domain.mapper.toUiModel
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import com.crossplatform.sdk.domain.repo.OtherPaymentMethodRepo
import com.crossplatform.sdk.presentation.UiState
import com.crossplatform.sdk.presentation.sharedContext.handleFetchStatus
import com.crossplatform.sdk.presentation.sharedContext.handlePaymentResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BNPLViewModel (
    private val repo: OtherPaymentMethodRepo
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<SelectedPaymentMethod>>>(UiState.Loading)
    val uiState : StateFlow<UiState<List<SelectedPaymentMethod>>> get() = _uiState
    private var allBanks = mutableStateOf<List<SelectedPaymentMethod>>(emptyList())
    val searchQuery = mutableStateOf("")

    val isBoxPayAnimationVisible = MutableStateFlow(false)
    val showWebview = MutableStateFlow(false)
    val url = mutableStateOf<String?>(null)
    val htmlString = mutableStateOf<String?>(null)

    init {
        loadBanksList()
    }

    fun loadBanksList() {
        viewModelScope.launch {
            _uiState.value = when (val response = repo.getPaymentMethods()) {
                is ApiResponse.Error -> {
                    UiState.Error(response.message)
                }
                ApiResponse.Loading -> {
                    UiState.Loading
                }
                is ApiResponse.Success -> {
                    allBanks.value = response.data.toUiModel("buynowpaylater")
                    UiState.Success(response.data.toUiModel("buynowpaylater"))
                }
            }
        }
    }

    fun onSearch(text: String) {
        searchQuery.value = text

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

    fun onClickRadio(instrumentValue: String) {

        val updatedList = allBanks.value.map {
            if (it.id == instrumentValue) {
                it.copy(isSelected = true)
            } else {
                it.copy(isSelected = false)
            }
        }
        _uiState.value = UiState.Success(updatedList)
    }

    fun postBNPLRequest(instrumentValue: String) {
        viewModelScope.launch {
            isBoxPayAnimationVisible.value = true
            val response = repo.initiatePayment(
                instrumentDetails = instrumentValue
            )
            handlePaymentResponse(
                response = response,
                onSetPaymentHtml = {html ->
                    htmlString.value = html
                    showWebview.value = true
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
                    showWebview.value = true
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
            val response = repo.fetchStatus()
            handleFetchStatus(
                response = response,
                setIsBoxPayAnimationVisible = {isBoxPayAnimationVisible.value = it}
            )
        }
    }
}