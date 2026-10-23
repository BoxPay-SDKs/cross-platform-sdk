package com.crossplatform.sdk.presentation.sharedContext

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.PaymentActions
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.parseErrorBodyAs
import com.crossplatform.sdk.domain.model.ApiErrorResponseModel
import com.crossplatform.sdk.domain.model.CardEncryption
import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import com.crossplatform.sdk.presentation.encryptCardData
import com.crossplatform.sdk.presentation.getStatus
import com.crossplatform.sdk.presentation.resolveErrorMessage
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.payment_failed_error_messages
import org.jetbrains.compose.resources.getString

private const val PAYMENT_MAX_ATTEMPTS_REACHED = "BE_1815"
private const val CHECKOUT_MAX_ATTEMPTS_REACHED = "BE_1816"
internal suspend fun handlePaymentResponse(
    response: ApiResponse<PaymentMethodPostResponse>,
    onRevolutPay: ((String, String) -> Unit)? = null,
    onSetNativeOtp : ((Int, Int, String, String) -> Unit)? = null,
    onSetPaymentUrl: ((String) -> Unit)? = null,
    onSetPaymentHtml: ((String) -> Unit)? = null,
    onNavigateToTimer: (() -> Unit)? = null,
    onOpenQr: ((String, Int) -> Unit)? = null,
    onOpenUpiIntent: ((String) -> Unit)? = null,
    errorMessage : String,
    setIsBoxPayAnimationVisible : (Boolean) -> Unit,
    cardEncryption: CardEncryption? = null,
) {
    when(response) {
        is ApiResponse.Success -> {
            val apiData = response.data
            val status = getStatus(apiData.status.status)
            val transactionId = apiData.transactionId

            CheckoutDetailsHandler.setStatusAndTransID(
                status = status.name,
                transactionId = transactionId
            )

            when (status) {
                TransactionStatusEnum.REQUIRESACTION -> {
                    val actions = apiData.actions
                    if (actions.isNullOrEmpty()) {
                        setIsBoxPayAnimationVisible(false)
                        onNavigateToTimer?.invoke()
                        return
                    }
                    val encryptAction = actions.firstOrNull { it.type == "encrypt" }

                    val finalActions = if (encryptAction != null) {
                        // 2. Encrypt card data
                        val jwe = encryptCardData(
                            encryptionKey = encryptAction.encryptionKey ?: "",
                            algo = encryptAction.algo ?: "",
                            keyId = encryptAction.keyId ?: "",
                            fingerprint = encryptAction.certificateFingerprint ?: "",
                            cardDetails = cardEncryption!!
                        )

                        // 3. Merge JWE into ALL other actions' data — mirrors web exactly
                        actions.map { action ->
                            action.copy(
                                data = action.data.orEmpty() +
                                        mapOf(encryptAction.outputField!! to jwe)
                            )
                        }
                    } else {
                        actions
                    }

                    handleActions(
                        actions = finalActions,
                        onRevolutPay = onRevolutPay,
                        onSetNativeOtp = onSetNativeOtp,
                        onSetPaymentUrl = onSetPaymentUrl,
                        onSetPaymentHtml = onSetPaymentHtml,
                        onNavigateToTimer = onNavigateToTimer,
                        onOpenQr = onOpenQr,
                        onOpenUpiIntent = onOpenUpiIntent,
                        setIsBoxPayAnimationVisible = setIsBoxPayAnimationVisible
                    )
                }

                TransactionStatusEnum.FAILED -> {
                    CheckoutDetailsHandler.setErrorMessage(
                        resolveErrorMessage(
                            reasonCode = apiData.status.reasonCode,
                            reason = apiData.status.reason,
                            fallback = errorMessage
                        )
                    )
                    CheckoutDetailsHandler.setSessionFailed()
                    setIsBoxPayAnimationVisible(false)
                }

                TransactionStatusEnum.SUCCESS -> {
                    CheckoutDetailsHandler.setTimeAndPaymentMethod(
                        timeStamp = response.data.transactionTimestampLocale,
                        paymentMethod = response.data.paymentMethod.brand ?: ""
                    )
                    CheckoutDetailsHandler.setSessionSuccess()
                    setIsBoxPayAnimationVisible(false)
                }

                TransactionStatusEnum.EXPIRED  -> {
                    CheckoutDetailsHandler.setSessionExpired()
                    setIsBoxPayAnimationVisible(false)
                }

                else -> {
                    CheckoutDetailsHandler.setErrorMessage(errorMessage)
                    CheckoutDetailsHandler.setSessionFailed()
                    setIsBoxPayAnimationVisible(false)
                }
            }
        }

        is ApiResponse.Error -> {
            val structuredError = response.parseErrorBodyAs<ApiErrorResponseModel>()

            when {
                structuredError?.reasonCode == PAYMENT_MAX_ATTEMPTS_REACHED -> {
                    val availableMethods = structuredError.fieldErrorItems
                        .filter { (it.message?.toIntOrNull() ?: 0) > 0 }
                        .map { it.fieldName }

                    CheckoutDetailsHandler.setRetryAvailableMethods(availableMethods)
                    CheckoutDetailsHandler.setIsPaymentMaxAttemptsReached()
                    setIsBoxPayAnimationVisible(false)
                }

                structuredError?.reasonCode == CHECKOUT_MAX_ATTEMPTS_REACHED -> {
                    CheckoutDetailsHandler.setIsCheckoutMaxAttemptsReached()
                    setIsBoxPayAnimationVisible(false)
                }

                errorMessage.contains("expired", ignoreCase = true) -> {
                    CheckoutDetailsHandler.setSessionExpired()
                    setIsBoxPayAnimationVisible(false)
                }

                else -> {
                    CheckoutDetailsHandler.setErrorMessage(
                        resolveErrorMessage(
                            reasonCode = structuredError?.reasonCode,
                            reason = structuredError?.message,
                            fallback = getString(Res.string.payment_failed_error_messages)
                        )
                    )
                    CheckoutDetailsHandler.setSessionFailed()
                    setIsBoxPayAnimationVisible(false)
                }
            }
        }

        else -> { /* no-op */ }
    }
}

private fun handleActions(
    actions: List<PaymentActions>,
    onRevolutPay: ((String, String) -> Unit)?,
    onSetNativeOtp: ((Int, Int, String, String) -> Unit)?,
    onSetPaymentUrl: ((String) -> Unit)?,
    onSetPaymentHtml: ((String) -> Unit)?,
    onNavigateToTimer: (() -> Unit)?,
    onOpenQr: ((String, Int) -> Unit)?,
    onOpenUpiIntent: ((String) -> Unit)?,
    setIsBoxPayAnimationVisible: (Boolean) -> Unit
) {
    // 1. nativeOtp can appear at ANY position — web checks secondAction but
    //    the real signal is its presence anywhere in the list.
    val nativeOtpAction = actions.firstOrNull { it.type == "nativeOtp" }
    if (nativeOtpAction != null) {
        // The trigger action is whichever non-nativeOtp action comes first
        val triggerAction = actions.firstOrNull { it.type != "nativeOtp" }
        onSetNativeOtp?.invoke(
            nativeOtpAction.minLength ?: 6,
            nativeOtpAction.maxLength ?: 6,
            triggerAction?.url ?: "",
            triggerAction?.htmlPageString ?: ""
        )
        return
    }

    // 2. Primary action = first non-encrypt action
    //    (web pre-processes encrypt separately; here we just skip it for routing)
    val primaryAction = actions.firstOrNull { it.type != "encrypt" }

    when (primaryAction?.type) {

        // ── HTML: WebView renders + auto-submits embedded form ──────────────
        "html" -> {
            val html = primaryAction.htmlPageString
            if (!html.isNullOrBlank()) {
                onSetPaymentHtml?.invoke(html)
            } else {
                setIsBoxPayAnimationVisible(false)
                onNavigateToTimer?.invoke()
            }
        }

        // ── REDIRECT: POST → self-submitting HTML form
        //              GET  → direct URL load ─────────────────────────────
        "redirect" -> {
            val url = primaryAction.url ?: ""
            val method = primaryAction.method?.uppercase() ?: ""

            if (url.isBlank()) {
                setIsBoxPayAnimationVisible(false)
                onNavigateToTimer?.invoke()
                return
            }

            if (method == "POST") {
                // Merge query-params already baked into the URL with the
                // explicit data map — mirrors web's addQueryParamsToObject()
                val mergedData = mergeUrlParamsWithData(url, primaryAction.data)
                val fields = mergedData.entries.joinToString("\n") { (k, v) ->
                    """<input type="hidden" name="${k.escapeHtml()}" value="${v.escapeHtml()}"/>"""
                }
                val cleanUrl = url.substringBefore("?")
                val html = """
                    <html><body onload="document.forms[0].submit()">
                      <form method="POST" action="${cleanUrl.escapeHtml()}">
                        $fields
                      </form>
                    </body></html>
                """.trimIndent()
                onSetPaymentHtml?.invoke(html)
            } else {
                // GET — just navigate to the URL
                onSetPaymentUrl?.invoke(url)
            }
        }

        // ── APP REDIRECT: UPI intent deep-link ──────────────────────────────
        "appRedirect" -> {
            val raw = primaryAction.url ?: ""
            if (raw.isNotBlank()) {
                onOpenUpiIntent?.invoke(raw)
            } else {
                setIsBoxPayAnimationVisible(false)
                onNavigateToTimer?.invoke()
            }
        }

        // ── QR CODE ─────────────────────────────────────────────────────────
        "qrCode" -> {
            onOpenQr?.invoke(
                primaryAction.content ?: "",
                primaryAction.expirySec ?: 0
            )
        }

        // ── INFO: Revolut Pay — token lives in primary, URL in next action ──
        "info" -> {
            val token = primaryAction.token ?: ""
            // Web grabs url from secondAction; we find the first non-info action
            val urlAction = actions.firstOrNull { it.type != "info" && it.type != "encrypt" }
            onRevolutPay?.invoke(token, urlAction?.url ?: "")
        }

        // ── UNKNOWN / NULL ──────────────────────────────────────────────────
        else -> {
            // Unknown type — navigate to timer (same as web fallback)
            setIsBoxPayAnimationVisible(false)
            onNavigateToTimer?.invoke()
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Mirrors web's addQueryParamsToObject():
 * extracts query params from the URL and merges them with the explicit data map.
 */
private fun mergeUrlParamsWithData(
    url: String,
    data: Map<String, String>?
): Map<String, String> {
    val result = mutableMapOf<String, String>()

    // Start with explicit data
    data?.let { result.putAll(it) }

    // Overlay with any params already in the URL
    if (url.contains("?")) {
        url.substringAfter("?")
            .split("&")
            .forEach { pair ->
                val idx = pair.indexOf('=')
                if (idx > 0) {
                    val key = pair.substring(0, idx).trim()
                    val value = pair.substring(idx + 1).trim()
                    result[key] = value
                }
            }
    }
    return result
}


/** Minimal HTML escaping for injected values. */
private fun String.escapeHtml(): String =
    replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")