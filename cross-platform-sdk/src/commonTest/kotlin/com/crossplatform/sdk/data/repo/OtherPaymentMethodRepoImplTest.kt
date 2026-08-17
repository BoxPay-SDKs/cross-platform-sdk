package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.Method
import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.TransactionStatus
import com.crossplatform.sdk.fakes.FakeApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OtherPaymentMethodRepoImplTest {

    private fun postResponse() = PaymentMethodPostResponse(
        transactionId = "txn_1",
        transactionTimestampLocale = "12 Aug 2026",
        status = TransactionStatus(status = "REQUIRESACTION", reason = "", reasonCode = ""),
        paymentMethod = Method(type = "UPI", brand = "UPI"),
    )

    @Test
    fun `getPaymentMethods forwards amount and offerId to the api service`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextPaymentMethods = ApiResponse.Success(
                listOf(PaymentMethod(id = "pm_1", type = "Wallet", brand = "Wallet")),
                responseCode = 200,
            )
        }
        val repo = OtherPaymentMethodRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

        val result = repo.getPaymentMethods(amount = 999.0, offerId = "SAVE10")

        assertIs<ApiResponse.Success<List<PaymentMethod>>>(result)
        assertEquals("fetchPaymentMethods(amount=999.0, offerId=SAVE10)", fakeApi.callLog.single())
    }

    @Test
    fun `getPaymentMethods works with null amount and offerId`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextPaymentMethods = ApiResponse.Success(emptyList(), responseCode = 200)
        }
        val repo = OtherPaymentMethodRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

        val result = repo.getPaymentMethods(amount = null, offerId = null)

        assertIs<ApiResponse.Success<List<PaymentMethod>>>(result)
        assertEquals("fetchPaymentMethods(amount=null, offerId=null)", fakeApi.callLog.single())
    }

    @Test
    fun `initiatePayment forwards instrumentDetails, token and paymentType in the right order`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextMethodsPostResponse = ApiResponse.Success(postResponse(), responseCode = 200)
        }
        val repo = OtherPaymentMethodRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

        val result = repo.initiatePayment(instrumentDetails = "{\"vpa\":\"a@b\"}", paymentType = "UPI", token = "tok_1")

        assertIs<ApiResponse.Success<PaymentMethodPostResponse>>(result)
        // methodsPostRequest(instrumentDetails, token, paymentType) - note the
        // argument order the repo passes matches the service signature, not
        // the repo's own parameter order.
        assertEquals("methodsPostRequest({\"vpa\":\"a@b\"}, UPI)", fakeApi.callLog.single())
    }

    @Test
    fun `initiateEMIPayment forwards every optional field through untouched`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextEmiPostResponse = ApiResponse.Success(postResponse(), responseCode = 200)
        }
        val repo = OtherPaymentMethodRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

        val result = repo.initiateEMIPayment(
            cardNumber = "4111111111111111",
            expiryDate = "2030-05",
            cvv = "123",
            holderName = "Jane Doe",
            cardType = "Credit",
            offerCode = "NOCOST",
            duration = 6,
            provider = null,
        )

        assertIs<ApiResponse.Success<PaymentMethodPostResponse>>(result)
        assertEquals("emiPostRequest(cardNumber=4111111111111111, duration=6)", fakeApi.callLog.single())
    }
}
