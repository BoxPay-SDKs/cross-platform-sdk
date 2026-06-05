package com.crossplatform.sdk.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChooseEmiModel(val cards: List<EmiCardGroup> = emptyList())

@Serializable
data class EmiCardGroup(
    val cardType: String,
    val banks: List<Bank> = emptyList(),
)

@Serializable
data class Bank(
    val iconUrl: String,
    val name: String,
    val percent: Double,
    val noCostApplied: Boolean,
    val lowCostApplied: Boolean,
    val emiList: List<Emi> = emptyList(),
    val cardLessEmiValue: String,
    val issuerBrand: String,
)

@Serializable
data class Emi(
    val duration: Int,
    val percent: Double,
    val amount: String,
    val totalAmount: String,
    val discount: String,
    val interestCharged: String,
    val noCostApplied: Boolean,
    val lowCostApplied: Boolean,
    val processingFee: String,
    val code: String,
    val netAmount: String,
)
