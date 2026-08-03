package com.crossplatform.sdk.domain.model

import kotlinx.serialization.Serializable

@Serializable
internal data class ChooseEmiModel(val cards: List<EmiCardGroup> = emptyList())

@Serializable
internal data class EmiCardGroup(
    val cardType: String,
    val banks: List<Bank> = emptyList(),
)

@Serializable
internal data class Bank(
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
internal data class Emi(
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
