package com.crossplatform.sdk.data.model.requestBody

import com.crossplatform.sdk.data.model.BrowserData
import com.crossplatform.sdk.data.model.DeviceDetails
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnalyticsRequest(
    @SerialName("browserData") val browserData: BrowserData,
    @SerialName("callerToken") val callerToken: String,
    @SerialName("uiEvent") val uiEvent: String,
    @SerialName("eventAttrs") val eventAttrs: EventAttrs,
    @SerialName("deviceDetails") val deviceDetails: DeviceDetails
)

@Serializable
data class EventAttrs(
    @SerialName("errorMessage") val errorMessage: String,
    @SerialName("screenName") val screenName: String
)