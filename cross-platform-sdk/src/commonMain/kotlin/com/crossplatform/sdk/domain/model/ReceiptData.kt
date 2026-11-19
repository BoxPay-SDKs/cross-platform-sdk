package com.crossplatform.sdk.domain.model

internal data class ReceiptData(
    val merchantName : String,
    val transactionId: String,
    val orderId: String,
    val status : String,
    val date: String,
    val time: String,
    val paymentMethod: String,
    val productName : String,
    val formattedAmount: String,
    val currencyCode: String,
)