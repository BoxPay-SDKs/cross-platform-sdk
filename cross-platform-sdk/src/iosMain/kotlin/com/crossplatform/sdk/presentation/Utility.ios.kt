package com.crossplatform.sdk.presentation

import com.crossplatform.sdk.data.model.BrowserData
import com.crossplatform.sdk.data.model.DeviceDetails
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIScreen
import platform.UIKit.UIDevice
import platform.Foundation.NSTimeZone
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.localTimeZone
import platform.Foundation.secondsFromGMT

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