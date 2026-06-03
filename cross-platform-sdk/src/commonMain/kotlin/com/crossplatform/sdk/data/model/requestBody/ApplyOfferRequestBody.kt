package com.crossplatform.sdk.data.model.requestBody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApplyOfferRequestBody(
    @SerialName("offerSearchRequest") val offerSearchRequest : OfferSearchRequest
) {
    @Serializable
    data class OfferSearchRequest(
        @SerialName("minAmount") val minAmount : Double,
        @SerialName("offers") val offers : List<String>
    )
}