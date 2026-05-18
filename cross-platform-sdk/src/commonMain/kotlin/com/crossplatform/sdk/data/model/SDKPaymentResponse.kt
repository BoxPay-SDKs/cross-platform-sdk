package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SDKPaymentResponse(
    @SerialName("status") val status: String,
    @SerialName("transactionId") val transactionId: String,
    @SerialName("inquiryToken") val inquiryToken : String
)