package com.crossplatform.sdk.domain.model

import com.crossplatform.sdk.data.model.AllowedPaymentMethods

data class MainScreenModel(
    val status: TransactionStatusEnum,
    val transactionId: String,
    val totalAmount : Double,
    val successfulTimeStamp : String,
    val successfulPaymentMethod : String,
    val currencySymbol : String,
    val currencyCode : String,
    val methodFlags: MethodFlags,
    val orderDetails: OrderDetails?,
    val sessionExpiryTimer : String,
    val revolutPublicKey : String?,
    val googlePayAdditionData : GooglePayAdditionData?,
    val applePayAdditionData : ApplePayAdditionData?
) {

    data class MethodFlags(
        val isUPIIntentVisible: Boolean = false,
        val isUPICollectVisible: Boolean = false,
        val isUPIQRVisible: Boolean = false,
        val isUPIVisible : Boolean = false,
        val isCardsVisible: Boolean = false,
        val isWalletVisible: Boolean = false,
        val isNetBankingVisible: Boolean = false,
        val isEMIVisible: Boolean = false,
        val isBNPLVisible: Boolean = false,
        val isUPIOtmIntentVisible: Boolean = false,
        val isUPIOtmCollectVisible: Boolean = false,
        val isUPIOtmQRVisible: Boolean = false,
        val isUPIOtmVisible : Boolean = false,
        val isGooglePayVisible : Boolean = false,
        val isApplePayVisible : Boolean = false,
        val isRevolutPayVisible : Boolean = false
    )

    data class OrderDetails(
        val totalItems: Int,
        val shippingAmount: Double,
        val taxAmount: Double,
        val subTotalAmount: Double,
        val items: List<OrderItemUiModel>
    )

    data class OrderItemUiModel(
        val imageUrl: String?,
        val imageTitle: String?,
        val imageQty: Int?,
        val amount: Double?
    )

    data class GooglePayAdditionData(
        val merchantId : String?,
        val merchantName : String?,
        val gateway : String?,
        val siteReference : String?,
        val allowedPaymentMethods : List<AllowedPaymentMethods>?
    )

    data class ApplePayAdditionData(
        val merchantName : String?,
        val gateway : String?,
        val siteReference : String?,
        val merchantCapabilities : List<String>?,
        val supportedNetworks : List<String>?
    )

}