package com.crossplatform.sdk.domain.mapper

import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.handler.UserDataHandler
import com.crossplatform.sdk.data.model.SessionDetails
import com.crossplatform.sdk.domain.model.MainScreenModel
import com.crossplatform.sdk.domain.model.MainScreenModel.OrderItemUiModel
import com.crossplatform.sdk.presentation.getStatus

fun SessionDetails.toUiModel(): MainScreenModel {
    val moneyObject = this.paymentDetails.money
    val status = getStatus(this.status)

    val enabledFields = configs.enabledFields

    fun isEnabled(field: String)  = enabledFields.any { it.field == field }
    fun isEditable(field: String) = enabledFields.find { it.field == field }?.editable == true

    val subscriptionDetails: List<Pair<String, String>>? = paymentDetails.subscriptionDetails?.let { data ->
        val billingCycle = data.billingCycle

        val frequency: String? = billingCycle?.let {
            if (it.count == 1) formatWords(it.billingTimeUnit)
            else "Every ${it.count} ${formatWords(it.billingCycleValue)}"
        }

        val validity: String? = when {
            !data.expiryDateLocale.isNullOrEmpty()         -> formatDate(data.expiryDateLocale.split(" ").firstOrNull() ?: "")
            !data.recurringExpiryDateLocale.isNullOrEmpty() -> formatDate(data.recurringExpiryDateLocale.split(" ").firstOrNull() ?: "")
            else                                            -> null
        }

        listOfNotNull(
            moneyObject.amount.takeIf { it != 0.0 }?.let { "To be paid now" to "$it" },
            data.maxAmountLocaleFull?.takeIf { it.isNotEmpty() }?.let { "Recurring Amount" to "Up to $it" },
            frequency?.let { "Frequency" to it },
            validity?.let { "Expiry Date" to it }
        )
    }

    CheckoutDetailsHandler.setSDKConfig(
        currencySymbol              = moneyObject.currencySymbol,
        currencyCode                = moneyObject.currencyCode,
        itemsLength                 = this.paymentDetails.order?.items?.sumOf { it.quantity ?: 1 } ?: 0,
        amount                      = paymentDetails.money.amount,
        amountBeforeSurcharge = paymentDetails.money.amount,
        buttonColor                 = merchantDetails.checkoutTheme.primaryButtonColor,
        buttonTextColor             = merchantDetails.checkoutTheme.buttonTextColor,
        merchantLogo                = merchantDetails.merchantLogo ?: "",
        merchantName                = merchantDetails.merchantName ?: "",
        headerColor                 = merchantDetails.checkoutTheme.headerColor,
        headerTextColor             = merchantDetails.checkoutTheme.headerTextColor,
        isShippingAddressEnabled    = isEnabled("SHIPPING_ADDRESS"),
        isShippingAddressEditable   = isEditable("SHIPPING_ADDRESS"),
        isFullNameEnabled           = isEnabled("SHOPPER_NAME"),
        isFullNameEditable          = isEditable("SHOPPER_NAME"),
        isEmailEnabled              = isEnabled("SHOPPER_EMAIL"),
        isEmailEditable             = isEditable("SHOPPER_EMAIL"),
        isPhoneEnabled              = isEnabled("SHOPPER_PHONE"),
        isPhoneEditable             = isEditable("SHOPPER_PHONE"),
        isPanEnabled                = isEnabled("SHOPPER_PAN"),
        isPanEditable               = isEditable("SHOPPER_PAN"),
        isDOBEnabled                = isEnabled("SHOPPER_DOB"),
        isDOBEditable               = isEditable("SHOPPER_DOB"),
        isOrderItemDetailsVisible   = isEnabled("ORDER_ITEM_DETAILS"),
        isSessionExpiryVisible      = isEnabled("TIMER"),
        isMerchantLogoVisible       = isEnabled("MERCHANT_LOGO"),
        isSubscriptionCheckout      = subscriptionDetails != null,
        errorMessage = "You may have cancelled the payment or there was a delay in response. Please retry.",
        subscription = subscriptionDetails
    )

    UserDataHandler.set(
        firstName = paymentDetails.shopper.firstName,
        lastName = paymentDetails.shopper.lastName,
        email = paymentDetails.shopper.email,
        uniqueId = paymentDetails.shopper.uniqueReference,
        dob = paymentDetails.shopper.dateOfBirth,
        pan = paymentDetails.shopper.panNumber,
        address1 = paymentDetails.shopper.deliveryAddress?.address1,
        address2 = paymentDetails.shopper.deliveryAddress?.address2,
        city = paymentDetails.shopper.deliveryAddress?.city,
        state = paymentDetails.shopper.deliveryAddress?.state,
        pincode = paymentDetails.shopper.deliveryAddress?.postalCode,
        labelName = paymentDetails.shopper.deliveryAddress?.labelName,
        labelType = paymentDetails.shopper.deliveryAddress?.labelType
    )

    var methodFlags = MainScreenModel.MethodFlags()
    this.configs.paymentMethods.forEach { method ->
        methodFlags = when (method.type) {
            "Upi" -> {
                when (method.brand) {
                    "UpiIntent" -> methodFlags.copy(isUPIIntentVisible = true, isUPIVisible = true)
                    "UpiCollect" -> methodFlags.copy(isUPICollectVisible = true, isUPIVisible = true)
                    "UpiQr" -> methodFlags.copy(isUPIQRVisible = true, isUPIVisible = true)
                    else -> methodFlags
                }
            }
            "UpiOneTimeMandate" -> {
                when (method.brand) {
                    "UpiIntentOtm" -> methodFlags.copy(isUPIOtmIntentVisible = true, isUPIOtmVisible = true)
                    "UpiCollectOtm" -> methodFlags.copy(isUPIOtmCollectVisible = true, isUPIOtmVisible = true)
                    "UpiQrOtm" -> methodFlags.copy(isUPIOtmQRVisible = true, isUPIOtmVisible = true)
                    else -> methodFlags
                }
            }
            "Card"           -> methodFlags.copy(isCardsVisible      = true)
            "Wallet"         -> methodFlags.copy(isWalletVisible     = true)
            "NetBanking"     -> methodFlags.copy(isNetBankingVisible = true)
            "Emi"            -> methodFlags.copy(isEMIVisible        = true)
            "BuyNowPayLater" -> methodFlags.copy(isBNPLVisible       = true)
            else             -> methodFlags
        }
    }

    val orderDetails: MainScreenModel.OrderDetails? = this.paymentDetails.order?.let { order ->
        MainScreenModel.OrderDetails(
            totalItems     = order.items?.sumOf { it.quantity ?: 1 } ?: 0,
            shippingAmount = order.shippingAmount ?: 0.0,
            taxAmount      = order.taxAmount ?: 0.0,
            subTotalAmount = order.originalAmount ?: 0.0,
            items          = order.items?.map { item ->
                OrderItemUiModel(
                    imageUrl    = item.imageUrl,
                    imageTitle  = item.itemName,
                    imageQty    = item.quantity,
                    amount = item.amountWithoutTax
                )
            } ?: emptyList()
        )
    }


    return MainScreenModel(
        status = status,
        transactionId = this.lastTransactionId ?: "",
        totalAmount = moneyObject.amount,
        currencySymbol = moneyObject.currencySymbol,
        currencyCode = moneyObject.currencyCode,
        methodFlags = methodFlags,
        orderDetails = orderDetails,
        sessionExpiryTimer = this.sessionExpiryTimestamp
    )
}

fun formatWords(text: String): String {
    return text.replace(Regex("([a-z])([A-Z])"), "$1 $2")
}

fun formatDate(dateStr: String): String {
    val datePart = dateStr.split(" ").firstOrNull() ?: return ""
    val parts = datePart.split("/")
    if (parts.size < 3) return ""

    val (day, month, year) = parts

    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    val monthName = months.getOrNull(month.toIntOrNull()?.minus(1) ?: return "") ?: return ""
    return "$day-$monthName-$year"
}