package com.crossplatform.sdk.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionDetails(
    @SerialName("configs") val configs: Configs,
    @SerialName("paymentDetails") val paymentDetails: PaymentDetails,
    @SerialName("merchantDetails") val merchantDetails: MerchantDetails,
    @SerialName("sessionExpiryTimestamp") val sessionExpiryTimestamp: String,
    @SerialName("status") val status: String,
    @SerialName("lastPaidAtTimestampLocale") val lastPaidAtTimestampLocale: String,
    @SerialName("lastTransactionId") val lastTransactionId: String
)

@Serializable
data class Configs(
    @SerialName("paymentMethods") val paymentMethods: List<PaymentMethod>,
    @SerialName("enabledFields") val enabledFields: List<EnabledFields>
)

@Serializable
data class EnabledFields(
    @SerialName("field") val field: String,
    @SerialName("editable") val editable: Boolean,
    @SerialName("mandatory") val mandatory: Boolean
)

@Serializable
data class PaymentDetails(
    @SerialName("context") val context: PaymentContext,
    @SerialName("money") val money: Money,
    @SerialName("shopper") val shopper: Shopper,
    @SerialName("subscriptionDetails") val subscriptionDetails: SubscriptionDetails? = null,
    @SerialName("order") val order: OrderDetails? = null
)

@Serializable
data class PaymentContext(
    @SerialName("countryCode") val countryCode: String,
    @SerialName("localeCode") val localeCode: String
)

@Serializable
data class Money(
    @SerialName("amountLocaleFull") val amountLocaleFull: String,
    @SerialName("currencySymbol") val currencySymbol: String,
    @SerialName("currencyCode") val currencyCode: String,
    @SerialName("amount") val amount: Double
)

@Serializable
data class Shopper(
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("phoneNumber") val phoneNumber: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("uniqueReference") val uniqueReference: String,
    @SerialName("deliveryAddress") val deliveryAddress: DeliveryAddress? = null,
    @SerialName("dateOfBirth") val dateOfBirth: String? = null,
    @SerialName("panNumber") val panNumber: String? = null
)

@Serializable
data class DeliveryAddress(
    @SerialName("address1") val address1: String? = null,
    @SerialName("address2") val address2: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("countryCode") val countryCode: String? = null,
    @SerialName("postalCode") val postalCode: String? = null,
    @SerialName("labelName") val labelName : String? = null,
    @SerialName("labelType") val labelType : String? = null
)

@Serializable
data class SubscriptionDetails(
    @SerialName("type") val type: String,
    @SerialName("maxAmountLocaleFull") val maxAmountLocaleFull: String
)

@Serializable
data class OrderDetails(
    @SerialName("shippingAmountLocaleFull") val shippingAmountLocaleFull: String? = null,
    @SerialName("taxAmountLocaleFull") val taxAmountLocaleFull: String? = null,
    @SerialName("originalAmountLocaleFull") val originalAmountLocaleFull: String? = null,
    @SerialName("items") val items: List<OrderItem>? = null
)

@Serializable
data class OrderItem(
    @SerialName("name") val name: String? = null,
    @SerialName("quantity") val quantity: Int? = null,
    @SerialName("price") val price: String? = null
)

@Serializable
data class MerchantDetails(
    @SerialName("checkoutTheme") val checkoutTheme: CheckoutTheme
)

@Serializable
data class CheckoutTheme(
    @SerialName("primaryButtonColor") val primaryButtonColor: String,
    @SerialName("buttonTextColor") val buttonTextColor: String,
    @SerialName("headerColor") val headerColor: String,
    @SerialName("headerTextColor") val headerTextColor: String
)

@Serializable
data class PaymentMethod(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("brand") val brand: String,
    @SerialName("title") val title: String,
    @SerialName("logoUrl") val logoUrl: String,
    @SerialName("instrumentTypeValue") val instrumentTypeValue: String,
    @SerialName("emiMethod") val emiMethod: EmiMethod? = null  // optional (?) = null
)

@Serializable
data class EmiMethod(
    @SerialName("brand") val brand: String,
    @SerialName("issuer") val issuer: String,
    @SerialName("duration") val duration: Int,
    @SerialName("effectiveInterestRate") val effectiveInterestRate: Double,
    @SerialName("merchantBorneInterestRate") val merchantBorneInterestRate: Double,
    @SerialName("issuerTitle") val issuerTitle: String,
    @SerialName("netAmountLocaleFull") val netAmountLocaleFull: String,
    @SerialName("totalAmountLocaleFull") val totalAmountLocaleFull: String,
    @SerialName("emiAmountLocaleFull") val emiAmountLocaleFull: String,
    @SerialName("merchantBorneInterestAmountLocaleFull") val merchantBorneInterestAmountLocaleFull: String,
    @SerialName("bankChargedInterestAmountLocaleFull") val bankChargedInterestAmountLocaleFull: String,
    @SerialName("interestChargedAmountLocaleFull") val interestChargedAmountLocaleFull: String,
    @SerialName("cardlessEmiProviderTitle") val cardlessEmiProviderTitle: String,
    @SerialName("cardlessEmiProviderValue") val cardlessEmiProviderValue: String,
    @SerialName("applicableOffer") val applicableOffer: ApplicableOffer? = null,
    @SerialName("processingFee") val processingFee: ProcessingFee? = null
)

@Serializable
data class ApplicableOffer(
    @SerialName("code") val code: String,
    @SerialName("discount") val discount: Discount
)

@Serializable
data class Discount(
    @SerialName("percentage") val percentage: Double,
    @SerialName("type") val type: String
)

@Serializable
data class ProcessingFee(
    @SerialName("amountLocaleFull") val amountLocaleFull: String
)