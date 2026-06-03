package com.crossplatform.sdk.domain.model

data class OfferItem(
    val code: String,
    val description: String,
    val terms : String,
    val discountType : String,
    val discountAmount: Double,
    val currencySymbol: String
)