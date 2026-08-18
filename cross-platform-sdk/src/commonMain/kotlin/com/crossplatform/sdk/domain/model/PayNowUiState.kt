package com.crossplatform.sdk.domain.model

internal sealed interface PayNowUiState {
    data object Idle : PayNowUiState
    data class Ready(val qrImage: String, val totalSeconds: Int) : PayNowUiState
    data object Expired : PayNowUiState
}