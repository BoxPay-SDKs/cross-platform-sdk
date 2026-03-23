package com.crossplatform.sdk.data.model


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MethodsPostRequest(
    @SerialName("browserData") val browserData: BrowserData,
    @SerialName("instrumentDetails") val instrumentDetails: MethodInstrumentDetails,
    @SerialName("shopper") val shopper: Shopper,
    @SerialName("deviceDetails") val deviceDetails: DeviceDetails
)

@Serializable
data class MethodInstrumentDetails(
    @SerialName("type") val type: String,
)