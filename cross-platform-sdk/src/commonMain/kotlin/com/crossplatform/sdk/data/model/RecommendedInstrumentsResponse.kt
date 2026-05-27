package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecommendedInstrumentsResponse(
    @SerialName("type") val type : String?,
    @SerialName("brand") val brand : String?,
    @SerialName("instrumentRef") val instrumentRef : String?,
    @SerialName("displayValue") val displayValue : String?,
    @SerialName("logoUrl") val logoUrl : String?,
    @SerialName("cardNickName") val cardNickName : String?
)