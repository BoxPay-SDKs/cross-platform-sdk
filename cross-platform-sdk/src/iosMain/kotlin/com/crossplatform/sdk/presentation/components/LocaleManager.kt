package com.crossplatform.sdk.presentation.components

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

fun createSettings(): Settings =
    NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)