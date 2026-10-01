package com.crossplatform.sdk.presentation.components

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocaleManager {
    private val _currentLanguage = MutableStateFlow<String?>(null)
    val currentLanguage: StateFlow<String?> = _currentLanguage.asStateFlow()

    fun setLanguage(code: String) {
        _currentLanguage.value = code
    }
}