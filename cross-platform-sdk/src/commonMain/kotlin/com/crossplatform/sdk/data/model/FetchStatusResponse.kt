package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FetchStatusResponse(
    @SerialName("status") val status: String,
    @SerialName("transactionId") val transactionId: String,
    @SerialName("reasonCode") val reasonCode: String,
    @SerialName("statusReason") val reason: String,
    @SerialName("transactionTimestampLocale") val transactionTimestampLocale: String,
    @SerialName("retryable") val retryable : Boolean,
    @SerialName("paymentMethod") val paymentMethod : PaymentMethod
) {
    @Serializable
    data class PaymentMethod(
        @SerialName("id") val id : String,
        @SerialName("type") val type : String? = null,
        @SerialName("brand") val brand : String? = null,
        @SerialName("subBrand") val subBrand : String? = null,
    )
}