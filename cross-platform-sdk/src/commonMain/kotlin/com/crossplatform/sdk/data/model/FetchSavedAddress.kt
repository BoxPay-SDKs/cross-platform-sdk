package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FetchSavedAddress(
    @SerialName("address1") val address1: String,
    @SerialName("address2") val address2: String? = null,       // string | null
    @SerialName("city") val city: String,
    @SerialName("state") val state: String,
    @SerialName("countryCode") val countryCode: String,
    @SerialName("postalCode") val postalCode: String,
    @SerialName("shopperRef") val shopperRef: String,
    @SerialName("addressRef") val addressRef: String,
    @SerialName("labelType") val labelType: String,
    @SerialName("labelName") val labelName: String? = null,     // string | null
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
    @SerialName("phoneNumber") val phoneNumber: String
)