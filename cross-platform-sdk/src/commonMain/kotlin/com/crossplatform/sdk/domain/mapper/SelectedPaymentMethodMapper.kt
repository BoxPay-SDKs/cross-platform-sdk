package com.crossplatform.sdk.domain.mapper

import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.domain.model.SelectedPaymentMethod

fun List<PaymentMethod>.toUiModel(type: String): List<SelectedPaymentMethod> {
    return this
        .filter { it.type.equals(type, ignoreCase = true) } // ✅ filter by type
        .mapNotNull { item ->

            val title = item.title?.trim() ?: ""

            if (title.isEmpty()) return@mapNotNull null

            SelectedPaymentMethod(
                type = item.type,
                id = item.id,
                displayName = title,
                displayValue = title,
                imageUrl = item.logoUrl,
                instrumentType = item.instrumentTypeValue,
                isLastUsed = false,
                isSelected = false
            )
        }
        .sortedBy { it.displayName.lowercase() }
}