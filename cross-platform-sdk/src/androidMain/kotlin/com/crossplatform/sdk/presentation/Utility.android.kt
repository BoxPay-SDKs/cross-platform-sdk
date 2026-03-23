package com.crossplatform.sdk.presentation

import com.crossplatform.sdk.data.model.BrowserData

import android.content.res.Resources
import android.os.Build
import com.crossplatform.sdk.data.model.DeviceDetails
import java.util.TimeZone

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