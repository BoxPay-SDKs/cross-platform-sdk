package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GooglePayExpressCheckoutResponse(
    @SerialName("apiVersion") val apiVersion: Int,
    @SerialName("apiVersionMinor") val apiVersionMinor: Int,
    @SerialName("paymentMethodData") val paymentMethodData: PaymentMethodData
)

@Serializable
internal data class PaymentMethodData(
    @SerialName("description") val description: String,
    @SerialName("info") val info: PaymentInfo,
    @SerialName("tokenizationData") val tokenizationData: TokenizationData,
    @SerialName("type") val type: String
)

@Serializable
internal data class PaymentInfo(
    @SerialName("assuranceDetails") val assuranceDetails: AssuranceDetails? = null,
    @SerialName("cardDetails") val cardDetails: String,
    @SerialName("cardFundingSource") val cardFundingSource: String? = null,
    @SerialName("cardNetwork") val cardNetwork: String
)

@Serializable
internal data class AssuranceDetails(
    @SerialName("accountVerified") val accountVerified: Boolean,
    @SerialName("cardHolderAuthenticated") val cardHolderAuthenticated: Boolean
)

@Serializable
internal data class TokenizationData(
    @SerialName("token")val token: String,
    @SerialName("type")val type: String
)