package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.model.CheckoutTheme
import com.crossplatform.sdk.data.model.Configs
import com.crossplatform.sdk.data.model.EnabledFields
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.MerchantDetails
import com.crossplatform.sdk.data.model.Method
import com.crossplatform.sdk.data.model.Money
import com.crossplatform.sdk.data.model.PaymentActions
import com.crossplatform.sdk.data.model.PaymentContext
import com.crossplatform.sdk.data.model.PaymentDetails
import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.SessionDetails
import com.crossplatform.sdk.data.model.Shopper
import com.crossplatform.sdk.data.model.TransactionStatus

/**
 * Shared builder helpers used by all RepoImpl integration tests.
 *
 * These build minimal but valid model instances so every test file doesn't
 * have to repeat the same boilerplate.
 */

internal fun buildMinimalSessionDetails(
    amount: Double = 100.0,
    currencyCode: String = "INR",
    status: String = "created",
    localeCode: String = "en",
    paymentMethods: List<PaymentMethod> = listOf(
        buildPaymentMethod(id = "pm_card", type = "Card", brand = "VISA"),
        buildPaymentMethod(id = "pm_upi",  type = "Upi",  brand = "UPI"),
    )
): SessionDetails = SessionDetails(
    configs = Configs(
        paymentMethods = paymentMethods,
        enabledFields  = listOf(EnabledFields(field = "email", editable = true, mandatory = false))
    ),
    paymentDetails = PaymentDetails(
        context = PaymentContext(countryCode = "IN", localeCode = localeCode),
        money   = Money(currencySymbol = "₹", currencyCode = currencyCode, amount = amount),
        shopper = Shopper(uniqueReference = "shopper_1")
    ),
    merchantDetails = MerchantDetails(
        merchantName = "TestMerchant",
        merchantLogo = null,
        checkoutTheme = buildMinimalCheckoutTheme()
    ),
    sessionExpiryTimestamp = null,
    status                 = status,
    lastPaidAtTimestamp    = null,
    lastTransactionId      = null,
    lastTransactionDetails = null
)

internal fun buildMinimalCheckoutTheme(): CheckoutTheme = CheckoutTheme(
    primaryButtonColor = "#000000",
    buttonTextColor = "#FFFFFF",
    headerColor = "#FFFFFF",
    headerTextColor = "#000000",
    focusedTextInputBorderColor = "#CCCCCC",
    unfocusedTextInputBorderColor = "#DDDDDD",
    payButtonFontSize = "16",
    font = "default",
    payButtonBorderRadius = "8"
)

internal fun buildPaymentMethod(
    id: String    = "pm_1",
    type: String  = "Card",
    brand: String = "VISA"
): PaymentMethod = PaymentMethod(id = id, type = type, brand = brand)

internal fun buildRequiresActionResponse(
    transactionId: String = "txn_test",
    actionType: String = "html",
    htmlContent: String? = "<html>pay</html>",
    url: String? = null,
    qrContent: String? = null,
    expirySec: Int? = null,
    paymentType: String = "Card",
    paymentBrand: String = "VISA"
): PaymentMethodPostResponse = PaymentMethodPostResponse(
    transactionId             = transactionId,
    transactionTimestampLocale = "01/06/2025 10:00:00",
    status = TransactionStatus(status = "RequiresAction", reason = "", reasonCode = ""),
    actions = listOf(
        PaymentActions(
            type           = actionType,
            htmlPageString = htmlContent,
            url            = url,
            content        = qrContent,
            expirySec      = expirySec
        )
    ),
    paymentMethod = Method(type = paymentType, brand = paymentBrand)
)

internal fun buildSuccessFetchStatus(
    transactionId: String = "txn_test",
    paymentType: String   = "Card"
): FetchStatusResponse = FetchStatusResponse(
    status                    = "APPROVED",
    transactionId             = transactionId,
    reasonCode                = "",
    reason                    = "",
    transactionTimestampLocale = "01/06/2025 10:00:00",
    retryable                 = false,
    paymentMethod             = FetchStatusResponse.PaymentMethod(id = "pm_1", type = paymentType)
)

internal fun buildFailedFetchStatus(
    reasonCode: String = "CARD_DECLINED",
    reason: String     = "Card declined"
): FetchStatusResponse = FetchStatusResponse(
    status                    = "FAILED",
    transactionId             = "txn_test",
    reasonCode                = reasonCode,
    reason                    = reason,
    transactionTimestampLocale = "01/06/2025 10:00:00",
    retryable                 = false,
    paymentMethod             = FetchStatusResponse.PaymentMethod(id = "pm_1", type = "Card")
)

internal fun buildExpiredFetchStatus(): FetchStatusResponse = FetchStatusResponse(
    status                    = "EXPIRED",
    transactionId             = "txn_test",
    reasonCode                = "",
    reason                    = "",
    transactionTimestampLocale = "01/06/2025 10:00:00",
    retryable                 = false,
    paymentMethod             = FetchStatusResponse.PaymentMethod(id = "pm_1", type = "Card")
)

internal fun buildRetryableFetchStatus(): FetchStatusResponse = FetchStatusResponse(
    status                    = "FAILED",
    transactionId             = "txn_test",
    reasonCode                = "RETRYABLE",
    reason                    = "Retryable failure",
    transactionTimestampLocale = "01/06/2025 10:00:00",
    retryable                 = true,
    paymentMethod             = FetchStatusResponse.PaymentMethod(id = "pm_1", type = "Card")
)
