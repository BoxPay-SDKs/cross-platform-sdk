package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppliedOfferResponse(
    @SerialName("originalAmount") val originalAmount : Double?,
    @SerialName("evaluatedOffers") val evaluatedOffers : List<EvaluatedOffers>?,
    @SerialName("finalAmount") val finalAmount : Double?
) {
    @Serializable
    data class EvaluatedOffers(
        @SerialName("title") val title : String?,
        @SerialName("description") val description : String?,
        @SerialName("appliedDiscountAmount") val appliedDiscountAmount : Double?
    )
}