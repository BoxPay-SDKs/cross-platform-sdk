package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NativeOtpResponse(
    @SerialName("success") val success : Boolean
)