package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethodPostResponse(
    @SerialName("transactionId") val transactionId: String,
    @SerialName("transactionTimestampLocale") val transactionTimestampLocale: String,
    @SerialName("status") val status: TransactionStatus,
    @SerialName("actions") val actions: List<PaymentActions>
)

@Serializable
data class TransactionStatus(
    @SerialName("status") val status: String,
    @SerialName("reason") val reason: String,
    @SerialName("reasonCode") val reasonCode: String
)

@Serializable
data class PaymentActions(
    @SerialName("method") val method: String,
    @SerialName("url") val url: String,
    @SerialName("type") val type: String,
    @SerialName("htmlPageString") val htmlPageString: String,
    @SerialName("content") val content: String
)