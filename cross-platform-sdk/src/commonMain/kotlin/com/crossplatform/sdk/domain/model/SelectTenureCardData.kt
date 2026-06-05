package com.crossplatform.sdk.domain.model

data class SelectTenureCardData(
    val duration: Int,
    val monthlyEmiAmount: String,
    val interest: Double,
    val interestCharged: String,
    val discount: String,
    val totalAmount: String,
    val debitedAmount: String,
    val isLowCostOffer: Boolean,
    val isNoCostOffer: Boolean,
    val isSelected: Boolean,
    val processingFee: String,
)