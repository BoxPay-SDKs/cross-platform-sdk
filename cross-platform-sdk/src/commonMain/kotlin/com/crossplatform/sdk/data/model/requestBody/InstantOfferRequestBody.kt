package com.crossplatform.sdk.data.model.requestBody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InstantOfferRequestBody(
    @SerialName("currency") val currencyCode : String,
    @SerialName("minAmount") val minAmount : Double,
    @SerialName("maxAmount") val maxAmount : Double
)