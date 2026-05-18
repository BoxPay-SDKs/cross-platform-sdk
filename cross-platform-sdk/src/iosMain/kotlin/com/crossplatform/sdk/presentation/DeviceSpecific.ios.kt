package com.crossplatform.sdk.presentation

import androidx.compose.runtime.Composable
import com.crossplatform.sdk.data.model.BrowserData
import com.crossplatform.sdk.data.model.DeviceDetails
import com.crossplatform.sdk.domain.model.AppLifecycleState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.UIKit.UIScreen
import platform.UIKit.UIDevice
import platform.Foundation.NSTimeZone
import platform.Foundation.NSLocale
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.localTimeZone
import platform.Foundation.secondsFromGMT
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIApplicationWillResignActiveNotification

actual fun getDeviceDetails(): DeviceDetails {
    return DeviceDetails(
        browser = "ios",
        platformVersion = UIDevice.currentDevice.systemVersion,
        deviceType = "Phone",
        deviceName = "iOS Device",
        deviceBrandName = "Apple"
    )
}

@OptIn(ExperimentalForeignApi::class)
actual fun getBrowserData(): BrowserData {
    return BrowserData(
        screenHeight = UIScreen.mainScreen.bounds.useContents {
            size.height.toInt().toString()
        },
        screenWidth = UIScreen.mainScreen.bounds.useContents {
            size.width.toInt().toString()
        },
        acceptHeader = "application/json",
        userAgentHeader = "KMP SDK",
        browserLanguage = NSLocale.currentLocale.languageCode ,
        ipAddress = "null",
        colorDepth = 24,
        javaEnabled = true,
        timeZoneOffSet = NSTimeZone.localTimeZone.secondsFromGMT.toInt() / 60,
        packageId = "com.boxpay.checkout.sdk"
    )
}

actual fun getInstalledUpiApps(context: Any?): List<String> {
    val schemes = mapOf(
        "gpay"    to "gpay://",
        "phonepe" to "phonepe://",
        "paytm"   to "paytmmp://"
    )
    return schemes.filter { (_, scheme) ->
        val url = NSURL.URLWithString(scheme)
        url != null && UIApplication.sharedApplication.canOpenURL(url)
    }.keys.toList()
}

@Composable
actual fun getPlatformContext(): Any? {
    return null
}

actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

actual fun currentYear(): Int {
    val components = NSCalendar.currentCalendar.components(
        NSCalendarUnitYear, NSDate()
    )
    return (components.year % 100).toInt()
}

actual fun currentMonth(): Int {
    val components = NSCalendar.currentCalendar.components(
        NSCalendarUnitMonth, NSDate()
    )
    return components.month.toInt()
}

actual fun launchUpiIntent(url: String, onFailure: (Throwable) -> Unit, onSuccess: () -> Unit) {
    val nsUrl = NSURL.URLWithString(url)
        ?: run {
            onFailure(IllegalArgumentException("Could not create NSURL from: $url"))
            return
        }

    val app = UIApplication.sharedApplication

    // canOpenURL requires the scheme declared in LSApplicationQueriesSchemes
    if (!app.canOpenURL(nsUrl)) {
        onFailure(IllegalStateException("No UPI app found to handle URL: $url"))
        return
    }

    // iOS 10+ async API
    app.openURL(
        url       = nsUrl,
        options   = emptyMap<Any?, Any>(),
        completionHandler = { success ->
            if (!success) {
                onFailure(IllegalStateException("openURL returned false for: $url"))
            } else {
                onSuccess()
            }
        }
    )
}

actual class AppLifecycleObserver actual constructor(private val onStateChange: (AppLifecycleState) -> Unit) {
    private val center = NSNotificationCenter.defaultCenter
    private val tokens = mutableListOf<Any>()

    actual fun start() {
        tokens += center.addObserverForName(
            name   = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue  = NSOperationQueue.mainQueue,
            usingBlock = { _ -> onStateChange(AppLifecycleState.Foreground) }
        )
        tokens += center.addObserverForName(
            name   = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue  = NSOperationQueue.mainQueue,
            usingBlock = { _ -> onStateChange(AppLifecycleState.Background) }
        )
        tokens += center.addObserverForName(
            name   = UIApplicationWillResignActiveNotification,
            `object` = null,
            queue  = NSOperationQueue.mainQueue,
            usingBlock = { _ -> onStateChange(AppLifecycleState.Inactive) }
        )
    }

    actual fun stop() {
        tokens.forEach { center.removeObserver(it) }
        tokens.clear()
    }
}