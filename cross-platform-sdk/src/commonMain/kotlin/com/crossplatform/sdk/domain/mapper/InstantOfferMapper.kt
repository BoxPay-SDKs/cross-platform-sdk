package com.crossplatform.sdk.domain.mapper

import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.InstantOfferResponse
import com.crossplatform.sdk.domain.model.OfferItem

fun List<InstantOfferResponse>.toUiModel(): List<OfferItem> {
    val checkoutDetails = CheckoutDetailsHandler.checkoutDetails
    return this.map { item ->
        OfferItem(
            code = item.code,
            description = item.description ?: item.title ?: "",
            terms = item.terms ?: "",
            discountType = if(item.discount.type.equals("flat", true)) "${checkoutDetails.currencySymbol}${item.discount.amount}" else "${item.discount.percentage}%",
            discountAmount = item.discount.amount ?: 0.0,
            currencySymbol = checkoutDetails.currencySymbol
        )
    }
}