package com.crossplatform.sdk.data.model.requestBody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EditSavedCardRequestBody(
    @SerialName("type") val type : String,
    @SerialName("cardNickName") val cardNickName : String
)