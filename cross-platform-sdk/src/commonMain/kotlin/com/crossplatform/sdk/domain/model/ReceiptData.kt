package com.crossplatform.sdk.domain.model

internal data class ReceiptData(
    val transactionId: String,
    val date: String,
    val time: String,
    val paymentMethod: String,
    val formattedAmount: String,
    val currencyCode: String,
)