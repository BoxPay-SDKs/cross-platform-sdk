package com.crossplatform.sdk.data.model.requestBody

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SurchargeRequestBody(
    @SerialName("discountedMoney") val discountedMoney : DiscountedMoney
) {

    @Serializable
    internal data class DiscountedMoney(
        @SerialName("amount") val amount : Double,
        @SerialName("currencyCode") val currencyCode : String
    )
}