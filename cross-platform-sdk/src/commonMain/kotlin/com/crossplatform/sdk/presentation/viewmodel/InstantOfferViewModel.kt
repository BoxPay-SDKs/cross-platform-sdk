package com.crossplatform.sdk.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import com.crossplatform.sdk.domain.repo.InstantOfferRepo
import kotlinx.coroutines.launch

class InstantOfferViewModel(
    private val repo: InstantOfferRepo,
    private val analyticsRepo : CallUIAnalyticsRepo
) : ViewModel() {

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