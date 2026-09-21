package com.crossplatform.sdk.presentation.components

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocaleManager(private val settings: Settings) { // Settings from multiplatform-settings
    private val _currentLanguage = MutableStateFlow(
        settings.getStringOrNull(KEY_LANGUAGE) // saved choice, if any
    )
    val currentLanguage: StateFlow<String?> = _currentLanguage.asStateFlow()

    fun setLanguage(code: String) {
        _currentLanguage.value = code
        settings.putString(KEY_LANGUAGE, code) // persist so it survives app restart
    }

    companion object {
        private const val KEY_LANGUAGE = "sdk_selected_language"
    }
}