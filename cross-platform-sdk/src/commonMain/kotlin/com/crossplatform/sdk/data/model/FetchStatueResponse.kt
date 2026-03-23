package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FetchStatusResponse(
    @SerialName("status") val status: String,
    @SerialName("transactionId") val transactionId: String,
    @SerialName("reasonCode") val reasonCode: String,
    @SerialName("reason") val reason: String,
    @SerialName("transactionTimestampLocale") val transactionTimestampLocale: String
)