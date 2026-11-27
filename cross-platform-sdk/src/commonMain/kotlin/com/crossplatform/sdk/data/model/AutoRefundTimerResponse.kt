package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AutoRefundTimerResponse(
    @SerialName("token") val token: String? = null,
    @SerialName("transactionId") val transactionId: String? = null,
    @SerialName("timerStarted") val timerStarted: Boolean = false,
    @SerialName("autoRefundAt") val autoRefundAt: String? = null,
    @SerialName("autoRefundTimeoutSeconds") val autoRefundTimeoutSeconds: Int = 0
)
