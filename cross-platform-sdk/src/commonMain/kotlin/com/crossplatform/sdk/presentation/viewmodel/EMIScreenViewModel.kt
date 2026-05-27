package com.crossplatform.sdk.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import com.crossplatform.sdk.domain.repo.EMIScreenRepo
import kotlinx.coroutines.launch

class EMIScreenViewModel(
    private val repo : EMIScreenRepo,
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