package com.crossplatform.sdk.presentation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.crossplatform.sdk.data.model.BrowserData

import android.content.res.Resources
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.crossplatform.sdk.data.model.DeviceDetails
import java.util.TimeZone
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.crossplatform.sdk.domain.model.AppLifecycleState
import java.util.Calendar


lateinit var appContext: Context
actual fun getDeviceDetails(): DeviceDetails {
    return DeviceDetails(
        browser = "android",
        platformVersion = Build.VERSION.SDK_INT.toString(),
        deviceType = "Phone",
        deviceName = "Android Device",
        deviceBrandName = Build.BRAND
    )
}

actual fun getBrowserData(): BrowserData {
    return BrowserData(
        screenHeight = Resources.getSystem().displayMetrics.heightPixels.toString(),
        screenWidth = Resources.getSystem().displayMetrics.widthPixels.toString(),
        acceptHeader = "application/json",
        userAgentHeader = "KMP SDK",
        browserLanguage = "en_US",
        ipAddress = "null",
        colorDepth = 24,
        javaEnabled = true,
        timeZoneOffSet = TimeZone.getDefault().rawOffset / 60000,
        packageId = "com.boxpay.checkout.sdk"
    )
}

actual fun getInstalledUpiApps(context: Any?): List<String> {
    val androidContext = context as Context
    val pm = androidContext.packageManager
    val packages = mapOf(
        "gpay"    to "com.google.android.apps.nbu.paisa.user",
        "paytm"   to "net.one97.paytm",
        "phonepe" to "com.phonepe.app"
    )
    return packages.filter { (_, pkg) ->
        try {
            pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) { false }
    }.keys.toList()
}

@Composable
actual fun getPlatformContext(): Any? {
    appContext = LocalContext.current
    return  LocalContext.current
}

actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}

actual fun currentYear(): Int {
    return Calendar.getInstance().get(Calendar.YEAR) % 100
}

actual fun currentMonth(): Int {
    return Calendar.getInstance().get(Calendar.MONTH) + 1
}

actual fun launchUpiIntent(url: String, onFailure: (Throwable) -> Unit, onSuccess: () -> Unit) {
    try {
        val uri    = url.toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // resolveActivity is the safe way to check before startActivity
        val canHandle = appContext.packageManager
            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null

        if (canHandle) {
            appContext.startActivity(intent)
            onSuccess()
        } else {
            onFailure(IllegalStateException("No UPI app found to handle intent: $url"))
        }
    } catch (e: Exception) {
        onFailure(e)
    }
}

actual class AppLifecycleObserver actual constructor(onStateChange: (AppLifecycleState) -> Unit) {
    private val observer = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            onStateChange(AppLifecycleState.Foreground)
        }
        override fun onStop(owner: LifecycleOwner) {
            onStateChange(AppLifecycleState.Background)
        }
    }

    actual fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
    }

    actual fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
    }
}

@Composable
actual fun BackHandler(onBack: () -> Unit) {
    BackHandler {
        onBack()
    }
}