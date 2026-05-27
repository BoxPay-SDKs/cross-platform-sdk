package com.crossplatform.sdk.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.FetchSavedAddress
import com.crossplatform.sdk.domain.repo.AddressScreenRepo
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import com.crossplatform.sdk.presentation.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AddressScreenViewModel(
    private val repo: AddressScreenRepo,
    private val analyticsRepo : CallUIAnalyticsRepo
) : ViewModel() {

    private val _savedList = MutableStateFlow<UiState<List<FetchSavedAddress>>>(UiState.Loading)
    val savedList : StateFlow<UiState<List<FetchSavedAddress>>> get() = _savedList
    val selectedSavedAddress = MutableStateFlow<String>("")


    init {
        val checkoutDetails = CheckoutDetailsHandler.checkoutDetails
        if(!checkoutDetails.shopperToken.isNullOrBlank()) {
            getSavedAddress()
        }
    }

    fun getSavedAddress() {
        viewModelScope.launch {
            val response = repo.getSavedAddress()
        }
    }

    fun deleteSavedAddress(addressRef : String) {
        viewModelScope.launch {
            val response = repo.deleteSavedAddress(addressRef = addressRef)
        }
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