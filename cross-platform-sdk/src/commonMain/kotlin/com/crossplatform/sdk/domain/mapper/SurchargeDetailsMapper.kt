package com.crossplatform.sdk.domain.mapper

import com.crossplatform.sdk.data.model.FetchSurchargeResponse
import com.crossplatform.sdk.domain.model.SurchargeModel

fun FetchSurchargeResponse.toUiModel() : List<SurchargeModel> {
    val expressCheckout = listOf("googlepay", "applepay", "revolutpay")
    return appliedCharges?.map { applied ->
        SurchargeModel(
            applicableOn   =
                if(expressCheckout.contains(applied.surchargeDetails?.network?.lowercase())) applied.surchargeDetails?.network?.lowercase() ?: ""
                else applied.surchargeDetails?.applicableOn?.lowercase() ?: "",
            title          = applied.surchargeDetails?.title ?: "",
            surchargeCode  = applied.surchargeDetails?.surchargeCode ?: "",
            network        = applied.surchargeDetails?.network ?: "",
            classification = applied.surchargeDetails?.classification ?: "",
            amount         = applied.calculatedSurchargeFee ?: 0.0
        )
    } ?: emptyList()
}