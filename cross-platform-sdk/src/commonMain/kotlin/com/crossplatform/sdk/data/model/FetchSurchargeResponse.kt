package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FetchSurchargeResponse(
    @SerialName("amountBeforeSurcharge") val amountBeforeSurcharge : AmountBeforeSurcharge?,
    @SerialName("appliedSurcharges") val appliedCharges : List<AppliedSurcharge>?,
    @SerialName("finalAmountAfterSurcharge") val finalAmountAfterMarriage :AmountBeforeSurcharge?
) {

    @Serializable
    data class AmountBeforeSurcharge(
        @SerialName("amount") val amount : Double?,
        @SerialName("currencyCode") val currencyCode : String?
    )

    @Serializable
    data class AppliedSurcharge(
        @SerialName("surchargeDetails") val surchargeDetails : SurchargeDetails?,
        @SerialName("calculatedSurchargeFee") val calculatedSurchargeFee : Double?
    )

    @Serializable
    data class SurchargeDetails(
        @SerialName("title") val title : String?,
        @SerialName("merchantId") val merchantId : String?,
        @SerialName("surchargeCode") val surchargeCode  : String?,
        @SerialName("applicableOn") val applicableOn : String?,
        @SerialName("network") val network : String?,
        @SerialName("classification") val classification : String?
    )
}