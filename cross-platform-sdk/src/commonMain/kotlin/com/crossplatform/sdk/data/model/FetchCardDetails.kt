package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FetchCardDetails(
    @SerialName("paymentMethod") val paymentMethod: CardPaymentMethod,
    @SerialName("methodEnabled") val methodEnabled: Boolean,
    @SerialName("issuerName") val issuerName: Issuer? = null,
    @SerialName("issuerTitle") val issuerTitle: String? = null
)

@Serializable
internal data class CardPaymentMethod(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("brand") val brand: String,
    @SerialName("issuer") val issuer: Issuer? = null,
    @SerialName("classification") val classification: String?
)

@Serializable
internal data class Issuer(
    @SerialName("code") val code : String?,
    @SerialName("title") val title : String?
)