package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InstantOfferResponse(
    @SerialName("title") val title : String?,
    @SerialName("description") val description : String?,
    @SerialName("terms") val terms : String?,
    @SerialName("code") val code : String,
    @SerialName("discount") val discount : Discount,
    @SerialName("enabled") val enabled : Boolean,
    @SerialName("criteria") val criteria : InstantOfferCriteria
) {
    @Serializable
    data class InstantOfferCriteria(
        @SerialName("applicableTo") val applicableTo : OfferApplicableTo,
        @SerialName("startDate") val startDate : String?,
        @SerialName("endDate") val endDate : String?
    )

    @Serializable
    data class OfferApplicableTo(
        @SerialName("paymentMethods") val paymentMethods : List<OfferPaymentMethod>?
    )

    @Serializable
    data class OfferPaymentMethod(
        @SerialName("type") val type : String? = null,
        @SerialName("brand") val brand : String? = null
    )

    @Serializable
    data class Discount(
        @SerialName("amount") val amount : Double? = null,
        @SerialName("percentage") val percentage : Double? = null,
        @SerialName("type") val type : String? = null
    )
}