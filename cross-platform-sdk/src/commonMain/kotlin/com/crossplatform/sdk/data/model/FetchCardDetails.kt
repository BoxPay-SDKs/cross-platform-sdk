package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FetchCardDetails(
    @SerialName("paymentMethod") val paymentMethod: CardPaymentMethod,
    @SerialName("methodEnabled") val methodEnabled: Boolean,
    @SerialName("issuerName") val issuerName: String? = null,
    @SerialName("issuerTitle") val issuerTitle: String? = null
)

@Serializable
data class CardPaymentMethod(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("brand") val brand: String,
    @SerialName("issuer") val issuer: String,
    @SerialName("classification") val classification: String
)