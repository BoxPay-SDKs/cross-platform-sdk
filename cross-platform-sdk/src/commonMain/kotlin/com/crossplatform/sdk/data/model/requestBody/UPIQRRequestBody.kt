package com.crossplatform.sdk.data.model.requestBody

import com.crossplatform.sdk.data.model.BrowserData
import com.crossplatform.sdk.data.model.DeviceDetails
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UPIQRRequestBody(
    @SerialName("browserData") val browserData: BrowserData,
    @SerialName("instrumentDetails") val instrumentDetails: Instrument,
    @SerialName("shopper") val shopper: ShopperRequest,
    @SerialName("deviceDetails") val deviceDetails: DeviceDetails,
) {
    @Serializable
    data class Instrument(
        @SerialName("type") val type : String  // 'upi/qr' | 'upiotm/qr'
    )
}