package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethodPostResponse(
    @SerialName("transactionId") val transactionId: String,
    @SerialName("transactionTimestampLocale") val transactionTimestampLocale: String,
    @SerialName("status") val status: TransactionStatus,
    @SerialName("actions") val actions: List<PaymentActions>? = null,
    @SerialName("paymentMethod") val paymentMethod: Method
)

@Serializable
data class TransactionStatus(
    @SerialName("status") val status: String,
    @SerialName("reason") val reason: String,
    @SerialName("reasonCode") val reasonCode: String
)

@Serializable
data class PaymentActions(
    @SerialName("method") val method: String? = null,
    @SerialName("url") val url: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("htmlPageString") val htmlPageString: String? = null,
    @SerialName("content") val content: String? = null
)

@Serializable
data class Method(
    @SerialName("type") val type: String?,
    @SerialName("brand") val brand : String?
)