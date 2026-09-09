package com.crossplatform.sdk.data.model.requestBody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class NativeOtpRequestBody(
    @SerialName("otpValue") val otpValue : String
)