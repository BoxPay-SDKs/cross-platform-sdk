package com.crossplatform.sdk.presentation.sharedContext

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.domain.repo.FetchStatusRepo
import com.crossplatform.sdk.presentation.currentTimeMillis
import crossplatformsdk.cross_platform_sdk.generated.resources.Res
import crossplatformsdk.cross_platform_sdk.generated.resources.auto_refund_timeout_error_message
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

private const val STATUS_POLL_INTERVAL_MS = 4000L
private const val DEFAULT_AUTO_REFUND_TIMEOUT_SECONDS = 30

/**
 * Shared state for the auto-refund (pending payment) feature, used by every payment method.
 *
 * [isEnabled] is set from the session response (`configs.autoRefundEnabled`); the whole flow
 * is skipped when it is false. [secondsLeft] drives the countdown shown under the loader.
 */
internal object AutoRefundHandler {
    var isEnabled: Boolean = false

    internal var isRunning: Boolean = false

    /** Clock used for the countdown deadline; replaced in tests so virtual time can be used. */
    internal var clock: () -> Long = { currentTimeMillis() }

    internal fun resetForTest() {
        isEnabled = false
        isRunning = false
        _secondsLeft.value = null
        clock = { currentTimeMillis() }
    }

    private val _secondsLeft = MutableStateFlow<Int?>(null)
    val secondsLeft: StateFlow<Int?> = _secondsLeft

    internal fun setSecondsLeft(seconds: Int?) {
        _secondsLeft.value = seconds
    }
}

private fun FetchStatusResponse.isPending() = status.equals("PENDING", ignoreCase = true)

/**
 * Drop-in replacement for "fetch status, then [handleFetchStatus]".
 *
 * If the status is PENDING and auto refund is enabled for the session, the server-side
 * auto-refund timer is started, a countdown is shown under the loader and the status is
 * polled every 4 seconds. If it is still PENDING when the timer ends, the payment is shown
 * as failed with the auto-refund message. Otherwise behaves exactly like before.
 *
 * [isPollingFlow] is for QR / UPI-collect style flows (polled while the shopper pays): the
 * result is processed with [handleUpiCollectFetchStatus] instead of [handleFetchStatus].
 */
internal suspend fun fetchStatusWithAutoRefund(
    fetchStatusRepo: FetchStatusRepo,
    setIsBoxPayAnimationVisible: (Boolean) -> Unit,
    onAutoRetry: () -> Unit,
    notifyComponentsHandler: (() -> Unit)? = null,
    isPollingFlow: Boolean = false
) {
    // A countdown is already resolving the status; don't start a second one.
    if (AutoRefundHandler.isRunning) return

    val response = safeFetchStatus(fetchStatusRepo)
    // Another overlapping call may have started the countdown while this fetch was in flight.
    if (AutoRefundHandler.isRunning) return

    val resolved: ApiResponse<FetchStatusResponse>? =
        if (AutoRefundHandler.isEnabled && response is ApiResponse.Success && response.data.isPending()) {
            AutoRefundHandler.isRunning = true // keep the overlap guard while the timer starts
            try {
                val timer = (safeStartTimer(fetchStatusRepo, response.data.transactionId)
                        as? ApiResponse.Success)?.data
                if (timer?.timerStarted == true) {
                    setIsBoxPayAnimationVisible(true) // loader (with the countdown) must be visible
                    runAutoRefundFlow(
                        fetchStatusRepo = fetchStatusRepo,
                        timeoutSeconds = timer.autoRefundTimeoutSeconds
                            .takeIf { it > 0 } ?: DEFAULT_AUTO_REFUND_TIMEOUT_SECONDS
                    )
                } else {
                    response
                }
            } finally {
                AutoRefundHandler.setSecondsLeft(null)
                AutoRefundHandler.isRunning = false
            }
        } else {
            response
        }

    if (resolved == null) {
        // Still PENDING when the timer ran out.
        CheckoutDetailsHandler.setErrorMessage(autoRefundTimeoutMessage())
        CheckoutDetailsHandler.setSessionFailed()
        setIsBoxPayAnimationVisible(false)
        notifyComponentsHandler?.invoke()
        return
    }

    if (isPollingFlow) {
        handleUpiCollectFetchStatus(
            response = resolved,
            setIsBoxPayAnimationVisible = setIsBoxPayAnimationVisible,
            notifyComponentsHandler = notifyComponentsHandler
        )
    } else {
        handleFetchStatus(
            response = resolved,
            setIsBoxPayAnimationVisible = setIsBoxPayAnimationVisible,
            onAutoRetry = onAutoRetry,
            notifyComponentsHandler = notifyComponentsHandler
        )
    }
}

/**
 * Runs the countdown for [timeoutSeconds] and polls the status every 4 seconds.
 * Returns the first non-PENDING status response, or null if the timer runs out first.
 * The countdown and the polling run in separate coroutines so a slow status call never
 * freezes the visible timer. The caller owns [AutoRefundHandler.isRunning] and clearing
 * the countdown.
 */
private suspend fun runAutoRefundFlow(
    fetchStatusRepo: FetchStatusRepo,
    timeoutSeconds: Int
): ApiResponse<FetchStatusResponse>? {
    val deadline = AutoRefundHandler.clock() + timeoutSeconds * 1000L

    val resolved = CompletableDeferred<ApiResponse<FetchStatusResponse>>()
    val result = coroutineScope {
        // Polling: every 4 seconds, stops as soon as the status is no longer PENDING.
        launch {
            while (isActive && !resolved.isCompleted) {
                delay(STATUS_POLL_INTERVAL_MS)
                val response = safeFetchStatus(fetchStatusRepo)
                if (response is ApiResponse.Success && !response.data.isPending()) {
                    resolved.complete(response)
                }
            }
        }
        // Countdown: ticks every second; one last status check when it reaches 0.
        launch {
            while (isActive && !resolved.isCompleted) {
                val left = secondsUntil(deadline)
                AutoRefundHandler.setSecondsLeft(left)
                if (left <= 0) break
                delay(1000L)
            }
            if (!resolved.isCompleted) {
                resolved.complete(safeFetchStatus(fetchStatusRepo))
            }
        }
        val finalResponse = resolved.await()
        this.coroutineContext.cancelChildren()
        finalResponse
    }
    return if (result is ApiResponse.Success && !result.data.isPending()) result else null
}

/** A status call that throws becomes an [ApiResponse.Error] instead of crashing the flow. */
private suspend fun safeFetchStatus(repo: FetchStatusRepo): ApiResponse<FetchStatusResponse> =
    try {
        repo.fetchStatus()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ApiResponse.Error(e.message ?: "status check failed")
    }

private suspend fun safeStartTimer(repo: FetchStatusRepo, transactionId: String) =
    try {
        repo.startAutoRefundTimer(transactionId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ApiResponse.Error(e.message ?: "auto refund timer failed")
    }

private fun secondsUntil(deadlineMillis: Long): Int {
    val remainingMs = deadlineMillis - AutoRefundHandler.clock()
    return if (remainingMs <= 0) 0 else ((remainingMs + 999) / 1000).toInt()
}

private suspend fun autoRefundTimeoutMessage(): String =
    try {
        getString(Res.string.auto_refund_timeout_error_message)
    } catch (e: Exception) {
        "If any amount is deducted or blocked, it will be refunded or released automatically, as applicable."
    }