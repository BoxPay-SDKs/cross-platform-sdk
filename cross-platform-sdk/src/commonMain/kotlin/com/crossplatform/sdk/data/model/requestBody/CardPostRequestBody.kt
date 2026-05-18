package com.crossplatform.sdk.data.model.requestBody

import com.crossplatform.sdk.data.model.BrowserData
import com.crossplatform.sdk.data.model.DeviceDetails
import com.crossplatform.sdk.data.model.Shopper
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CardPostRequestBody(
    @SerialName("browserData") val browserData: BrowserData,
    @SerialName("instrumentDetails") val instrumentDetails: InstrumentDetails,
    @SerialName("shopper") val shopper: Shopper,
    @SerialName("deviceDetails") val deviceDetails: DeviceDetails,
    @SerialName("oneTimePayment") val oneTimePayment: Boolean? = null  // conditional field
) {
    @Serializable
    data class InstrumentDetails(
        @SerialName("type") val type: String,
        @SerialName("card") val card: CardDetails,
        @SerialName("saveInstrument") val saveInstrument: Boolean? = null  // conditional field
    )

    @Serializable
    data class CardDetails(
        @SerialName("number") val number: String,
        @SerialName("expiry") val expiry: String,
        @SerialName("cvc") val cvc: String,
        @SerialName("holderName") val holderName: String,
        @SerialName("nickName") val nickName: String? = null  // conditional field
    )
}


@Serializable
data class SavedCardPostRequestBody(
    @SerialName("browserData") val browserData: BrowserData,
    @SerialName("instrumentDetails") val instrumentDetails: Instrument,
    @SerialName("shopper") val shopper: Shopper,
    @SerialName("deviceDetails") val deviceDetails: DeviceDetails,
    @SerialName("oneTimePayment") val oneTimePayment: Boolean? = null  // conditional field
) {
    @Serializable
    data class Instrument(
        @SerialName("type") val type : String, // 'card/token'
        @SerialName("card") val card : SavedCard
    )

    @Serializable
    data class SavedCard(
        @SerialName("instrumentRef") val instrumentRef: String
    )
}