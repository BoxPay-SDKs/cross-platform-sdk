package com.crossplatform.sdk.data.model.requestBody

import com.crossplatform.sdk.data.model.BrowserData
import com.crossplatform.sdk.data.model.DeviceDetails
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmiPostRequestBody(
    @SerialName("browserData") val browserData: BrowserData,
    @SerialName("instrumentDetails") val instrumentDetails: InstrumentDetails,
    @SerialName("shopper") val shopper: ShopperRequest,
    @SerialName("deviceDetails") val deviceDetails: DeviceDetails,
    @SerialName("offers") val offers : List<String>? = null
) {
    @Serializable
    data class InstrumentDetails(
        @SerialName("type") val type: String,
        @SerialName("card") val card: CardPostRequestBody.CardDetails? = null,
        @SerialName("emi") val emi: Emi? = null
    )

    @Serializable
    data class Emi(
        @SerialName("duration") val duration: Int? = null,
        @SerialName("provider") val provider : String? = null
    )
}