package com.crossplatform.sdk.domain.model

internal data class WebViewState(
    val isLoading: Boolean = true,
    val currentUrl: String = "",
)