package com.crossplatform.sdk.domain.mapper

import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.InstantOfferResponse
import com.crossplatform.sdk.domain.model.OfferItem

fun List<InstantOfferResponse>.toUiModel(): List<OfferItem> {
    val (currencySymbol, _) = CheckoutDetailsHandler.currencyFlow.value
    return this.map { item ->
        OfferItem(
            code = item.code,
            description = item.description ?: item.title ?: "",
            terms = item.terms ?: "",
            discountType = item.discount.type ?: "",
            discountAmount = item.discount.amount ?: 0.0,
            currencySymbol = currencySymbol,
            discountPercent = item.discount.percentage ?: 0.0,
            applicableOn = item.criteria.applicableTo.paymentMethods?.get(0)?.brand ?: "",
            expiryDate = null
        )
    }
}