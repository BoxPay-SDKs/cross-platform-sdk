package com.crossplatform.sdk.data.model.requestBody

import com.crossplatform.sdk.data.model.BrowserData
import com.crossplatform.sdk.data.model.DeviceDetails
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UPIIntentRequestBody(
    @SerialName("browserData") val browserData: BrowserData,
    @SerialName("instrumentDetails") val instrumentDetails: Instrument,
    @SerialName("shopper") val shopper: ShopperRequest,
    @SerialName("deviceDetails") val deviceDetails: DeviceDetails,
) {
    @Serializable
    data class Instrument(
        @SerialName("type") val type : String, // 'upi/intent' | 'upiotm/intent'
        @SerialName("upiAppDetails") val upiAppDetails : UPIAppDetails?
    )

    @Serializable
    data class UPIAppDetails(
        @SerialName("upiApp") val upiApp : String
    )
}