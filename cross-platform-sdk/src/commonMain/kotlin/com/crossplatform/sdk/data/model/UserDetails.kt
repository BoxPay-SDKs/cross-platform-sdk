package com.crossplatform.sdk.data.model

// src/commonMain/kotlin/com/crossplatform/sdk/model/UserData.kt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDetails(
    // ─── Personal Info ───────────────────────────
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("dob") val dob: String? = null,
    @SerialName("pan") val pan: String? = null,
    @SerialName("uniqueId") val uniqueId: String,

    // ─── Phone ───────────────────────────────────
    @SerialName("completePhoneNumber") val completePhoneNumber: String? = null,
    @SerialName("phoneCode") val phoneCode: String,

    // ─── Address ─────────────────────────────────
    @SerialName("address1") val address1: String? = null,
    @SerialName("address2") val address2: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("countryCode") val countryCode: String? = null,
    @SerialName("countryName") val countryName: String? = null,
    @SerialName("pincode") val pincode: String? = null,
    @SerialName("labelType") val labelType: String? = null,
    @SerialName("labelName") val labelName: String? = null,

    // Custom Fields -------------
    @SerialName("customFields") val customFields: List<CustomFields> = emptyList()
)