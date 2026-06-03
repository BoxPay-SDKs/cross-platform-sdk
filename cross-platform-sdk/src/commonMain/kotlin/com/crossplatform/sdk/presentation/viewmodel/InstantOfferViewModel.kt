package com.crossplatform.sdk.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.domain.mapper.toUiModel
import com.crossplatform.sdk.domain.model.OfferItem
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import com.crossplatform.sdk.domain.repo.InstantOfferRepo
import com.crossplatform.sdk.presentation.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class InstantOfferViewModel(
    private val repo: InstantOfferRepo,
    private val analyticsRepo : CallUIAnalyticsRepo
) : ViewModel() {

    private val _offerState = MutableStateFlow<UiState<List<OfferItem>>>(UiState.Loading)
    val offerState : StateFlow<UiState<List<OfferItem>>> get() = _offerState
    init {
        loadOffer()
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

    fun loadOffer() {
        val checkoutDetails = CheckoutDetailsHandler.checkoutDetails
        viewModelScope.launch {
            when(val response = repo.getOffers(checkoutDetails.amount, checkoutDetails.amount)) {
                is ApiResponse.Success -> {
                    _offerState.value = UiState.Success(response.data.toUiModel())
                }
                is ApiResponse.Error -> {
                    _offerState.value = UiState.Error(response.message)
                }
                is ApiResponse.Loading -> {
                    _offerState.value = UiState.Loading
                }
            }
        }
    }
}