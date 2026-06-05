package com.crossplatform.sdk.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.domain.mapper.toUiModel
import com.crossplatform.sdk.domain.model.Bank
import com.crossplatform.sdk.domain.model.ChooseEmiModel
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import com.crossplatform.sdk.domain.repo.OtherPaymentMethodRepo
import com.crossplatform.sdk.presentation.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EMIScreenViewModel(
    private val repo : OtherPaymentMethodRepo,
    private val analyticsRepo : CallUIAnalyticsRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ChooseEmiModel>>(UiState.Loading)
    val uiState : StateFlow<UiState<ChooseEmiModel>> get() = _uiState
    val emiBankList = mutableStateOf(ChooseEmiModel(emptyList()))
    val selectedCard = mutableStateOf("Credit Card")
    val searchText = mutableStateOf("")
    val selectedFilter = mutableStateOf("")

    val selectedOthers = mutableStateOf("")

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

    }

    fun onEditSearchText(text : String) {
        searchText.value = text
    }

    fun onToggleFilter(selectedFilter : String) {

    }

    fun onSelectedOthersOption(selected : String) {

    }

    fun onProceedWithOther() {

    }

    fun onClickBank(selectedBank : Bank) {

    }
}