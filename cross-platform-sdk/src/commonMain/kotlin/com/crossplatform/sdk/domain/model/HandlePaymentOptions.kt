package com.crossplatform.sdk.domain.model

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.TransactionStatus

data class HandlePaymentOptions(
    val response: ApiResponse<PaymentMethodPostResponse>,
    val upiId: String? = null,
    val checkoutDetailsErrorMessage: String,
    val onSetStatus: (TransactionStatus) -> Unit,
    val onSetTransactionId: (String) -> Unit,
    val onSetPaymentUrl: ((String) -> Unit)? = null,
    val onSetPaymentHtml: ((String) -> Unit)? = null,
    val onSetFailedMessage: ((String) -> Unit)? = null,
    val onShowFailedModal: (() -> Unit)? = null,
    val onShowSuccessModal: ((String) -> Unit)? = null,
    val onShowSessionExpiredModal: (() -> Unit)? = null,
    val onNavigateToTimer: ((String) -> Unit)? = null,
    val onOpenQr: ((String) -> Unit)? = null,
    val onOpenUpiIntent: ((String) -> Unit)? = null,
    val setLoading: (Boolean) -> Unit
)