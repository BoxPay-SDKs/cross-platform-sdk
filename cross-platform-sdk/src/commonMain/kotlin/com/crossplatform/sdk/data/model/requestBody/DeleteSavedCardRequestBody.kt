package com.crossplatform.sdk.data.model.requestBody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DeleteSavedCardRequestBody(
    @SerialName("type") val type : String,
    @SerialName("instrumentRef") val instrumentRef : String
)