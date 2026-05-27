package com.crossplatform.sdk.data.model.requestBody

import com.crossplatform.sdk.data.model.BrowserData
import com.crossplatform.sdk.data.model.DeviceDetails
import com.crossplatform.sdk.data.model.Shopper
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UPICollectRequestBody(
    @SerialName("browserData") val browserData: BrowserData,
    @SerialName("instrumentDetails") val instrumentDetails: Instrument,
    @SerialName("shopper") val shopper: Shopper,
    @SerialName("deviceDetails") val deviceDetails: DeviceDetails,
) {
    @Serializable
    data class Instrument(
        @SerialName("type") val type : String, // 'upi/collect' | 'upiotm/collect'
        @SerialName("upi") val upi : UPIDetails,
        @SerialName("saveInstrument") val saveInstrument: Boolean?,
    )

    @Serializable
    data class UPIDetails(
        @SerialName("instrumentRef") val instrumentRef: String? = null,
        @SerialName("shopperVpa") val shopperVpa: String? = null
    )
}