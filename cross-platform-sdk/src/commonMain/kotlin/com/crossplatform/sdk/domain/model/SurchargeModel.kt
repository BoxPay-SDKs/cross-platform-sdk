package com.crossplatform.sdk.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SurchargeModel(
    val applicableOn : String,
    val title : String,
    val surchargeCode : String,
    val network : String,
    val classification : String,
    val amount : Double
)