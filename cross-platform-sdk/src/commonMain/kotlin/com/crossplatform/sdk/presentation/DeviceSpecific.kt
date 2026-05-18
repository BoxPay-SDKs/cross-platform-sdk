package com.crossplatform.sdk.presentation

import androidx.compose.runtime.Composable
import com.crossplatform.sdk.data.model.BrowserData
import com.crossplatform.sdk.data.model.DeviceDetails
import com.crossplatform.sdk.domain.model.AppLifecycleState

expect fun getBrowserData(): BrowserData
expect fun getDeviceDetails(): DeviceDetails

expect fun getInstalledUpiApps(context: Any?): List<String>

@Composable
expect fun getPlatformContext(): Any?

expect fun currentTimeMillis(): Long
expect fun currentYear(): Int
expect fun currentMonth(): Int

expect fun launchUpiIntent(url: String, onFailure: (Throwable) -> Unit, onSuccess : ()-> Unit)

expect class AppLifecycleObserver(
    onStateChange: (AppLifecycleState) -> Unit
) {
    fun start()
    fun stop()
}