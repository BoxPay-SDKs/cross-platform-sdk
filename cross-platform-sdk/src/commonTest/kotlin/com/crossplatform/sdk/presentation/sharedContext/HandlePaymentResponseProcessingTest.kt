package com.crossplatform.sdk.presentation.sharedContext

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.Method
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.TransactionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * POSTED / PROCESSING responses to the payment (initiate) call must open the
 * Payment Successful page, just like SUCCESS, but keep PROCESSING as the
 * recorded status so the page can show "Processing".
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HandlePaymentResponseProcessingTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        CheckoutDetailsHandler.resetToDefault()
    }

    @AfterTest
    fun tearDown() {
        CheckoutDetailsHandler.resetToDefault()
        Dispatchers.resetMain()
    }

    private fun response(status: String, transactionId: String = "txn_1") =
        ApiResponse.Success(
            PaymentMethodPostResponse(
                transactionId = transactionId,
                transactionTimestampLocale = "01/06/2025 10:00:00",
                status = TransactionStatus(status = status, reason = "", reasonCode = ""),
                actions = null,
                paymentMethod = Method(type = "Card", brand = "VISA"),
            ),
            responseCode = 200,
        )

    private suspend fun handle(
        status: String,
        animationCalls: MutableList<Boolean> = mutableListOf(),
        onNotify: (() -> Unit)? = null,
    ) = handlePaymentResponse(
        response = response(status),
        errorMessage = "error",
        setIsBoxPayAnimationVisible = { animationCalls.add(it) },
        notifyComponentsHandler = onNotify,
    )

    @Test
    fun `POSTED shows the success page and records PROCESSING`() = runTest {
        handle("POSTED")

        val details = CheckoutDetailsHandler.checkoutDetails
        assertTrue(details.isPaymentSuccessful)
        assertFalse(details.isPaymentFailed)
        assertEquals("PROCESSING", details.status)
        assertEquals("txn_1", details.transactionId)
    }

    @Test
    fun `PROCESSING shows the success page and records PROCESSING`() = runTest {
        handle("PROCESSING")

        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertEquals("PROCESSING", CheckoutDetailsHandler.checkoutDetails.status)
    }

    @Test
    fun `PROCESSING stores the payment method brand for the success page`() = runTest {
        handle("POSTED")

        assertEquals("VISA", CheckoutDetailsHandler.successfulTimestampFlow.value.second)
    }

    @Test
    fun `PROCESSING hides the loading animation and notifies components`() = runTest {
        val animationCalls = mutableListOf<Boolean>()
        var notified = false

        handle("POSTED", animationCalls) { notified = true }

        assertEquals(listOf(false), animationCalls)
        assertTrue(notified)
    }

    @Test
    fun `APPROVED still records SUCCESS`() = runTest {
        handle("APPROVED")

        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertEquals("SUCCESS", CheckoutDetailsHandler.checkoutDetails.status)
    }

    @Test
    fun `FAILED does not show the success page`() = runTest {
        handle("FAILED")

        assertFalse(CheckoutDetailsHandler.checkoutDetails.isPaymentSuccessful)
        assertTrue(CheckoutDetailsHandler.checkoutDetails.isPaymentFailed)
    }
}
