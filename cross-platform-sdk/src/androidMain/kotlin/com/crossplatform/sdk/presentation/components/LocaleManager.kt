package com.crossplatform.sdk.presentation.components

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

fun createSettings(context: Context): Settings =
    SharedPreferencesSettings(context.getSharedPreferences("sdk_prefs", Context.MODE_PRIVATE))