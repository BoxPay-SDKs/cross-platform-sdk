package com.crossplatform.sdk.domain.model

data class MainScreenModel(
    val status: TransactionStatusEnum,
    val transactionId: String,
    val totalAmount : Double,
    val currencySymbol : String,
    val currencyCode : String,
    val methodFlags: MethodFlags,
    val orderDetails: OrderDetails?,
    val sessionExpiryTimer : String
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
        val isUPIOtmQRVisible: Boolean = false
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

}