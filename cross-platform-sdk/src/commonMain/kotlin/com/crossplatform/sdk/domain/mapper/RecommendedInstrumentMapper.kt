package com.crossplatform.sdk.domain.mapper

import com.crossplatform.sdk.data.model.RecommendedInstrumentsResponse
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod

fun List<RecommendedInstrumentsResponse>.toUiModel(): List<SelectedPaymentMethod> {
    return this.map { item ->
        SelectedPaymentMethod(
            type                = item.type ?: "",
            id                  = item.instrumentRef ?: "",
            displayName         = item.cardNickName ?: item.displayValue ?: "",
            displayValue        = item.displayValue ?: "",
            imageUrl             = item.logoUrl ?: "///",
            instrumentType = item.instrumentRef ?: "",
            isSelected          = false,
            isLastUsed          = false
        )
    }
}