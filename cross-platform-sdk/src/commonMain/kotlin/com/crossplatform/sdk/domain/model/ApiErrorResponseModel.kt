package com.crossplatform.sdk.domain.model

import kotlinx.serialization.Serializable

@Serializable
internal data class ApiErrorResponseModel(
    val errorCode: String? = null,
    val message: String? = null,
    val timestamp: String? = null,
    val fieldErrorItems: List<FieldErrorItem> = emptyList(),
    val retryable: Boolean = false,
    val reasonCode: String? = null
)

@Serializable
internal data class FieldErrorItem(
    val fieldName: String,
    val fieldErrorCode: String? = null,
    val message: String? = null
)