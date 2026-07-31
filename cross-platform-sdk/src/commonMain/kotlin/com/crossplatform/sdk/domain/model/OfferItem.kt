package com.crossplatform.sdk.domain.model

internal data class OfferItem(
    val code: String,
    val description: String,
    val terms : String,
    val discountType : String,
    val discountPercent : Double,
    val applicableOn : String,
    val discountAmount: Double,
    val currencySymbol: String,
    val expiryDate : String?
)