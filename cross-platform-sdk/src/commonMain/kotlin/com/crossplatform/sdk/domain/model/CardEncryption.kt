package com.crossplatform.sdk.domain.model

internal data class CardEncryption(
    val number: String,
    val expiryMonth: String,
    val expiryYear: String,
    val holderName: String,
    val cvv: String
)