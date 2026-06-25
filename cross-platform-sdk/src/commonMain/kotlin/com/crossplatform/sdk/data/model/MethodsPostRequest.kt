package com.crossplatform.sdk.data.model


import com.crossplatform.sdk.data.model.requestBody.ShopperRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MethodsPostRequest(
    @SerialName("browserData") val browserData: BrowserData,
    @SerialName("instrumentDetails") val instrumentDetails: MethodInstrumentDetails,
    @SerialName("shopper") val shopper: ShopperRequest,
    @SerialName("deviceDetails") val deviceDetails: DeviceDetails
)

@Serializable
data class MethodInstrumentDetails(
    @SerialName("type") val type: String,
)