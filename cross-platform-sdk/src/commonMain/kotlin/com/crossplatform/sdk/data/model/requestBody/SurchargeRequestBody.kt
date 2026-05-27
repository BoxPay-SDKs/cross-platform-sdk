package com.crossplatform.sdk.data.model.requestBody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SurchargeRequestBody(
    @SerialName("discountedMoney") val discountedMoney : DiscountedMoney
) {

    @Serializable
    data class DiscountedMoney(
        @SerialName("amount") val amount : Double,
        @SerialName("currencyCode") val currencyCode : String
    )
}