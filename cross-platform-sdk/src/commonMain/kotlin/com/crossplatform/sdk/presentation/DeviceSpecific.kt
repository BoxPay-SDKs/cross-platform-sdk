package com.crossplatform.sdk.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.crossplatform.sdk.data.model.BrowserData
import com.crossplatform.sdk.data.model.DeviceDetails
import com.crossplatform.sdk.domain.handler.ExpressCheckoutPaymentHandler
import com.crossplatform.sdk.domain.model.AppLifecycleState

internal expect fun getBrowserData(): BrowserData
internal expect fun getDeviceDetails(): DeviceDetails

internal expect fun getInstalledUpiApps(context: Any?): List<String>

@Composable
internal expect fun getPlatformContext(): Any?

internal expect fun currentTimeMillis(): Long
internal expect fun currentYear(): Int
internal expect fun currentMonth(): Int

internal expect fun launchUpiIntent(url: String, onFailure: (Throwable) -> Unit, onSuccess : ()-> Unit)

internal expect class AppLifecycleObserver(
    onStateChange: (AppLifecycleState) -> Unit
) {
    fun start()
    fun stop()
}

@Composable
internal expect fun BackHandler(onBack: () -> Unit)

internal expect fun isTabletDevice(): Boolean

internal expect fun base64ToImageBitmap(base64: String): ImageBitmap

// commonMain
@Composable
internal expect fun rememberExpressCheckoutPaymentHandler(): ExpressCheckoutPaymentHandler

internal expect fun formatAmount(amount: Double, minDecimals: Int = 0, maxDecimals: Int = 2): String

// commonMain
internal expect class QrImageSaver {
    suspend fun saveBase64Image(base64: String, fileName: String): Result<Unit>
}

@Composable
internal expect fun rememberQrImageSaver(): QrImageSaver