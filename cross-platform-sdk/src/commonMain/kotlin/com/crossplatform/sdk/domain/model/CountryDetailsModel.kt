package com.crossplatform.sdk.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CountryDetailsModel(
    @SerialName("isdCode") val isdCode: String,
    @SerialName("fullName") val fullName: String,
    @SerialName("threeLetterCode") val threeLetterCode: String,
    @SerialName("phoneNumberLength") val phoneNumberLength: List<Int>
)