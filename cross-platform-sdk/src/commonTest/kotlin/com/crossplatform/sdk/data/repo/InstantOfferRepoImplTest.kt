package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.AppliedOfferResponse
import com.crossplatform.sdk.data.model.InstantOfferResponse
import com.crossplatform.sdk.fakes.FakeApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InstantOfferRepoImplTest {

    private fun offer(code: String = "SAVE10") = InstantOfferResponse(
        title = "Save 10%", description = null, terms = null, code = code,
        discount = InstantOfferResponse.Discount(amount = null, percentage = 10.0, type = "PERCENTAGE"),
        enabled = true,
        criteria = InstantOfferResponse.InstantOfferCriteria(
            applicableTo = InstantOfferResponse.OfferApplicableTo(paymentMethods = emptyList()),
            startDate = null, endDate = null,
        ),
    )

    @Test
    fun `getOffers forwards min and max amount to the api service`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextOffer = ApiResponse.Success(listOf(offer()), responseCode = 200)
        }
        val repo = InstantOfferRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

        val result = repo.getOffers(minAmount = 100.0, maxAmount = 5000.0)

        assertIs<ApiResponse.Success<List<InstantOfferResponse>>>(result)
        assertEquals("getOffer(min=100.0, max=5000.0)", fakeApi.callLog.single())
    }

    @Test
    fun `getOffers propagates an error response`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextOffer = ApiResponse.Error(message = "no offers available")
        }
        val repo = InstantOfferRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

        val result = repo.getOffers(minAmount = 0.0, maxAmount = 100.0)

        assertIs<ApiResponse.Error>(result)
    }

    @Test
    fun `applyOffer forwards the offer id list and min amount`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextApplyOffer = ApiResponse.Success(
                AppliedOfferResponse(originalAmount = 1000.0, evaluatedOffers = emptyList(), finalAmount = 900.0),
                responseCode = 200,
            )
        }
        val repo = InstantOfferRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

        val result = repo.applyOffer(offerId = listOf("SAVE10", "WELCOME50"), minAmount = 500.0)

        assertIs<ApiResponse.Success<AppliedOfferResponse>>(result)
        assertEquals(900.0, result.data.finalAmount)
        assertEquals("applyOffer([SAVE10, WELCOME50], min=500.0)", fakeApi.callLog.single())
    }
}
