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
    @SerialName("lastPaidAtTimestamp") val lastPaidAtTimestamp: String?,
    @SerialName("lastTransactionId") val lastTransactionId: String?
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
    @SerialName("type") val type: String?,
    @SerialName("billingCycle") val billingCycle : SubscriptionBillingCycle?,
    @SerialName("billingDuration") val billingDuration : SubscriptionBillingDuration?,
    @SerialName("nextBillingDateLocale") val nextBillingDateLocale : String? = null,
    @SerialName("expiryDateLocale") val expiryDateLocale : String? = null,
    @SerialName("recurringExpiryDateLocale") val recurringExpiryDateLocale : String? = null,
    @SerialName("maxAmountLocaleFull") val maxAmountLocaleFull: String? = null
)

@Serializable
data class SubscriptionBillingCycle(
    @SerialName("billingTimeUnit") val billingTimeUnit : String,
    @SerialName("count") val count : Int,
    @SerialName("billingCycleValue") val billingCycleValue : String
)

@Serializable
data class SubscriptionBillingDuration(
    @SerialName("type") val type : String,
    @SerialName("noOfCycles") val noOfCycles : Int
)

@Serializable
data class OrderDetails(
    @SerialName("shippingAmount") val shippingAmount: Double? = null,
    @SerialName("taxAmount") val taxAmount: Double? = null,
    @SerialName("originalAmount") val originalAmount: Double? = null,
    @SerialName("items") val items: List<OrderItem>? = null
)

@Serializable
data class OrderItem(
    @SerialName("id") val id : String? = null,
    @SerialName("itemName") val itemName: String? = null,
    @SerialName("quantity") val quantity: Int? = null,
    @SerialName("imageUrl") val imageUrl : String? = null,
    @SerialName("amountWithoutTax") val amountWithoutTax: Double? = null
)

@Serializable
data class MerchantDetails(
    @SerialName("merchantName") val merchantName : String?,
    @SerialName("logoUrl") val merchantLogo : String?,
    @SerialName("checkoutTheme") val checkoutTheme: CheckoutTheme,
    @SerialName("customFields") val customFields : List<CustomFields>
)

@Serializable
data class CustomFields(
    @SerialName("fieldName") val fieldName : String?,
    @SerialName("placeHolderText") val placeHolderText : String?,
    @SerialName("fieldType") val fieldType : String?,
    @SerialName("validation") val validation : String?,
    @SerialName("validationParams") val validationParams : Map<String, String>?,
    @SerialName("dropDownOptions") val dropDownOptions : List<String>?,
    @SerialName("mandatory") val mandatory : Boolean,
    @SerialName("fieldValue") val fieldValue : String? = null
)

@Serializable
data class CheckoutTheme(
    @SerialName("primaryButtonColor") val primaryButtonColor: String,
    @SerialName("buttonTextColor") val buttonTextColor: String,
    @SerialName("headerColor") val headerColor: String,
    @SerialName("headerTextColor") val headerTextColor: String,
    @SerialName("inputBorderColor") val focusedTextInputBorderColor: String,
    @SerialName("inputFocusBorderColor") val unfocusedTextInputBorderColor: String,
    @SerialName("payBtnFontSize") val payButtonFontSize : String,
    @SerialName("font") val font : String,
    @SerialName("payBtnBorderRadius") val payButtonBorderRadius : String
)

@Serializable
data class PaymentMethod(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("brand") val brand: String,
    @SerialName("title") val title: String,
    @SerialName("logoUrl") val logoUrl: String,
    @SerialName("instrumentTypeValue") val instrumentTypeValue: String,
    @SerialName("applicableOffers") val applicableOffer: List<ApplicableOffer>? = null,
    @SerialName("emiMethod") val emiMethod: EmiMethod? = null
)

@Serializable
data class EmiMethod(
    @SerialName("brand") val brand: String? = null,
    @SerialName("issuer") val issuer: String? = null,
    @SerialName("duration") val duration: Int? = null,
    @SerialName("effectiveInterestRate") val effectiveInterestRate: Double? = null,
    @SerialName("merchantBorneInterestRate") val merchantBorneInterestRate: Double? = null,
    @SerialName("issuerTitle") val issuerTitle: String? = null,
    @SerialName("processingFee") val processingFee : ProcessingFee? = null,
    @SerialName("netAmountLocaleFull") val netAmountLocaleFull: String? = null,
    @SerialName("totalAmountLocaleFull") val totalAmountLocaleFull: String? = null,
    @SerialName("emiAmountLocaleFull") val emiAmountLocaleFull: String? = null,
    @SerialName("merchantBorneInterestAmountLocaleFull") val merchantBorneInterestAmountLocaleFull: String? = null,
    @SerialName("bankChargedInterestAmountLocaleFull") val bankChargedInterestAmountLocaleFull: String? = null,
    @SerialName("interestChargedAmountLocaleFull") val interestChargedAmountLocaleFull: String? = null,
    @SerialName("cardlessEmiProviderTitle") val cardlessEmiProviderTitle: String? = null,
    @SerialName("cardlessEmiProviderValue") val cardlessEmiProviderValue: String? = null,
)

@Serializable
data class ProcessingFee(
    @SerialName("amountLocaleFull") val amountLocaleFull : String?
)

@Serializable
data class ApplicableOffer(
    @SerialName("code") val code: String,
    @SerialName("title") val title: String,
    @SerialName("discount") val discount : Discount?
)

@Serializable
data class Discount(
    @SerialName("type") val type : String?
)