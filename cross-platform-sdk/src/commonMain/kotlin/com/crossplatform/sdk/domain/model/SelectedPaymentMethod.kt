package com.crossplatform.sdk.domain.model

data class SelectedPaymentMethod(
    val type : String,
    val id: String,
    val displayName: String,
    val displayValue : String,
    val imageUrl: String,
    val instrumentType : String,
    val isLastUsed : Boolean?,
    val isSelected : Boolean?
)