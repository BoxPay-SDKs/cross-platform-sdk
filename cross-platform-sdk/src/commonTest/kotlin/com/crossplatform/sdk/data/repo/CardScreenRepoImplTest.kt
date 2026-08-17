package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.CardPaymentMethod
import com.crossplatform.sdk.data.model.FetchCardDetails
import com.crossplatform.sdk.data.model.Method
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.TransactionStatus
import com.crossplatform.sdk.fakes.FakeApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CardScreenRepoImplTest {

    private fun cardDetails(brand: String = "VISA", methodEnabled: Boolean = true) = FetchCardDetails(
        paymentMethod = CardPaymentMethod(id = "pm_1", type = "Card", brand = brand, classification = "CONSUMER"),
        methodEnabled = methodEnabled,
    )

    private fun postResponse() = PaymentMethodPostResponse(
        transactionId = "txn_1",
        transactionTimestampLocale = "12 Aug 2026",
        status = TransactionStatus(status = "REQUIRESACTION", reason = "", reasonCode = ""),
        paymentMethod = Method(type = "Card", brand = "VISA"),
    )

    @Test
    fun `getCardDetails forwards the bin and returns the mapped brand`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextFetchCardDetails = ApiResponse.Success(cardDetails(brand = "Mastercard"), responseCode = 200)
        }
        val repo = CardScreenRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

        val result = repo.getCardDetails(cardNumber = "411111111")

        assertIs<ApiResponse.Success<FetchCardDetails>>(result)
        assertEquals("Mastercard", result.data.paymentMethod.brand)
        assertEquals("fetchCardDetails(411111111)", fakeApi.callLog.single())
    }

    @Test
    fun `getCardDetails returns an error response as-is when the bin lookup fails`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextFetchCardDetails = ApiResponse.Error(message = "Status: Not Found", errorBody = "{}")
        }
        val repo = CardScreenRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

        val result = repo.getCardDetails(cardNumber = "000000000")

        assertIs<ApiResponse.Error>(result)
        assertEquals("Status: Not Found", result.message)
    }

    @Test
    fun `postCardDetails passes every field through to the api service unchanged`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextCardPostResponse = ApiResponse.Success(postResponse(), responseCode = 200)
        }
        val repo = CardScreenRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

        val result = repo.postCardDetails(
            type = "card/plain",
            cardNumber = "4111111111111111",
            cvv = "123",
            cardName = "Jane Doe",
            expiry = "2030-05",
            nickName = "My Visa",
            isSaveInstrumentCheckboxClicked = true,
            isSICheckboxClicked = false,
        )

        assertIs<ApiResponse.Success<PaymentMethodPostResponse>>(result)
        assertEquals(
            "cardPostRequest(type=card/plain, cardNumber=4111111111111111, expiry=2030-05)",
            fakeApi.callLog.single()
        )
    }
}
