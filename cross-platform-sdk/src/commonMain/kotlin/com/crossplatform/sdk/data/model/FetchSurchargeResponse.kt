package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FetchSurchargeResponse(
    @SerialName("amountBeforeSurcharge") val amountBeforeSurcharge : AmountBeforeSurcharge?,
    @SerialName("appliedSurcharges") val appliedCharges : List<AppliedSurcharge>?,
    @SerialName("finalAmountAfterSurcharge") val finalAmountAfterMarriage :AmountBeforeSurcharge?
) {

    @Serializable
    internal data class AmountBeforeSurcharge(
        @SerialName("amount") val amount : Double?,
        @SerialName("currencyCode") val currencyCode : String?
    )

    @Serializable
    internal data class AppliedSurcharge(
        @SerialName("surchargeDetails") val surchargeDetails : SurchargeDetails?,
        @SerialName("calculatedSurchargeFee") val calculatedSurchargeFee : Double?
    )

    @Serializable
    internal data class SurchargeDetails(
        @SerialName("title") val title : String?,
        @SerialName("surchargeCode") val surchargeCode  : String? = null,
        @SerialName("applicableOn") val applicableOn : String? = null,
        @SerialName("network") val network : String? = null,
        @SerialName("classification") val classification : String? = null
    )
}