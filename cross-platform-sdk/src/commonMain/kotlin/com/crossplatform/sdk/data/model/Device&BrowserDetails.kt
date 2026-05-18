package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BrowserData(
    @SerialName("screenHeight") val screenHeight: String,
    @SerialName("screenWidth") val screenWidth: String,
    @SerialName("acceptHeader") val acceptHeader: String,
    @SerialName("userAgentHeader") val userAgentHeader: String,
    @SerialName("browserLanguage") val browserLanguage: String,
    @SerialName("ipAddress") val ipAddress: String,
    @SerialName("colorDepth") val colorDepth: Int,
    @SerialName("javaEnabled") val javaEnabled: Boolean,
    @SerialName("timeZoneOffSet") val timeZoneOffSet: Int,
    @SerialName("packageId") val packageId: String
)

@Serializable
data class DeviceDetails(
    @SerialName("browser") val browser: String,
    @SerialName("platformVersion") val platformVersion: String,
    @SerialName("deviceType") val deviceType: String,
    @SerialName("deviceName") val deviceName: String,
    @SerialName("deviceBrandName") val deviceBrandName: String
)