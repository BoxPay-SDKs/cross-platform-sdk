package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.CheckoutTheme
import com.crossplatform.sdk.data.model.Configs
import com.crossplatform.sdk.data.model.DeliveryAddress
import com.crossplatform.sdk.data.model.FetchSurchargeResponse
import com.crossplatform.sdk.data.model.MerchantDetails
import com.crossplatform.sdk.data.model.Method
import com.crossplatform.sdk.data.model.Money
import com.crossplatform.sdk.data.model.PaymentContext
import com.crossplatform.sdk.data.model.PaymentDetails
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.RecommendedInstrumentsResponse
import com.crossplatform.sdk.data.model.SessionDetails
import com.crossplatform.sdk.data.model.Shopper
import com.crossplatform.sdk.data.model.TransactionStatus
import com.crossplatform.sdk.fakes.FakeApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * [MainScreenRepoImpl] is a thin pass-through over 8 different [ApiService]
 * endpoints — one test per endpoint, mirroring the shape already proven out
 * in [FetchStatusRepoImplTest] and [CardScreenRepoImplTest].
 */
class MainScreenRepoImplTest {

    private fun minimalSessionDetails() = SessionDetails(
        configs = Configs(paymentMethods = emptyList(), enabledFields = emptyList()),
        paymentDetails = PaymentDetails(
            context = PaymentContext(countryCode = "IN", localeCode = "en-IN"),
            money = Money(currencySymbol = "\u20b9", currencyCode = "INR", amount = 100.0),
            shopper = Shopper(
                firstName = null, lastName = null, email = null,
                uniqueReference = "shopper_1", deliveryAddress = DeliveryAddress(),
            ),
            subscriptionDetails = null,
            order = null,
        ),
        merchantDetails = MerchantDetails(
            merchantName = "Test Merchant",
            merchantLogo = null,
            checkoutTheme = CheckoutTheme(
                primaryButtonColor = "#000000", buttonTextColor = "#FFFFFF", headerColor = "#000000",
                headerTextColor = "#FFFFFF", focusedTextInputBorderColor = "#CCCCCC",
                unfocusedTextInputBorderColor = "#DDDDDD", payButtonFontSize = "16", font = "Inter",
                payButtonBorderRadius = "8",
            ),
            customFields = emptyList(),
        ),
        sessionExpiryTimestamp = "",
        status = "NOACTION",
        lastPaidAtTimestamp = null,
        lastTransactionId = null,
        lastTransactionDetails = null,
    )

    private fun postResponse() = PaymentMethodPostResponse(
        transactionId = "txn_1",
        transactionTimestampLocale = "12 Aug 2026",
        status = TransactionStatus(status = "REQUIRESACTION", reason = "", reasonCode = ""),
        paymentMethod = Method(type = "UPI", brand = "UPI"),
    )

    private fun repo(fakeApi: FakeApiService) = MainScreenRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

    @Test
    fun `getSessionDetails returns the session from the api service`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextSessionDetails = ApiResponse.Success(minimalSessionDetails(), responseCode = 200)
        }

        val result = repo(fakeApi).getSessionDetails()

        assertIs<ApiResponse.Success<SessionDetails>>(result)
        assertEquals("Test Merchant", result.data.merchantDetails.merchantName)
        assertEquals("getSessionDetails()", fakeApi.callLog.single())
    }

    @Test
    fun `getSessionDetails propagates an error, e_g_ an expired session`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextSessionDetails = ApiResponse.Error(message = "Session expired")
        }

        val result = repo(fakeApi).getSessionDetails()

        assertIs<ApiResponse.Error>(result)
        assertEquals("Session expired", result.message)
    }

    @Test
    fun `postUpiIntentRequest forwards type and upiApp`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextUpiIntentPostResponse = ApiResponse.Success(postResponse(), responseCode = 200)
        }

        val result = repo(fakeApi).postUpiIntentRequest(type = "upi/intent", upiApp = "gpay")

        assertIs<ApiResponse.Success<PaymentMethodPostResponse>>(result)
        assertEquals("upiIntentPostRequest(upi/intent, gpay)", fakeApi.callLog.single())
    }

    @Test
    fun `postUpiCollectRequest forwards vpa and saveInstrument`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextUpiCollectPostResponse = ApiResponse.Success(postResponse(), responseCode = 200)
        }

        val result = repo(fakeApi).postUpiCollectRequest(
            type = "upi/collect", instrumentRef = null, shopperVpa = "user@upi", saveInstrument = true,
        )

        assertIs<ApiResponse.Success<PaymentMethodPostResponse>>(result)
        assertEquals("upiCollectPostRequest(upi/collect, user@upi)", fakeApi.callLog.single())
    }

    @Test
    fun `fetchRecommendedInstruments returns the saved instruments list`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextRecommendedInstruments = ApiResponse.Success(
                listOf(RecommendedInstrumentsResponse(type = "Card", brand = "VISA", instrumentRef = "ref_1", displayValue = "•••• 1111", logoUrl = null, cardNickName = null)),
                responseCode = 200,
            )
        }

        val result = repo(fakeApi).fetchRecommendedInstruments()

        assertIs<ApiResponse.Success<List<RecommendedInstrumentsResponse>>>(result)
        assertEquals("ref_1", result.data.single().instrumentRef)
    }

    @Test
    fun `postSavedCardRequest forwards the instrument ref and SI checkbox state`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextSavedCardPostResponse = ApiResponse.Success(postResponse(), responseCode = 200)
        }

        val result = repo(fakeApi).postSavedCardRequest(instrumentRef = "ref_1", isSICheckboxChecked = true)

        assertIs<ApiResponse.Success<PaymentMethodPostResponse>>(result)
        assertEquals("savedCardPostRequest(ref_1)", fakeApi.callLog.single())
    }

    @Test
    fun `getSurcharge forwards amount and currencyCode`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextSurcharge = ApiResponse.Success(
                FetchSurchargeResponse(amountBeforeSurcharge = null, appliedCharges = emptyList(), finalAmountAfterMarriage = null),
                responseCode = 200,
            )
        }

        val result = repo(fakeApi).getSurcharge(amount = 1999.0, currencyCode = "INR")

        assertIs<ApiResponse.Success<FetchSurchargeResponse>>(result)
        assertEquals("getSurcharge(amount=1999.0, currencyCode=INR)", fakeApi.callLog.single())
    }

    @Test
    fun `deleteSavedCard forwards the card id`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextDeleteSavedCard = ApiResponse.Success(
                RecommendedInstrumentsResponse(type = "Card", brand = "VISA", instrumentRef = "ref_1", displayValue = null, logoUrl = null, cardNickName = null),
                responseCode = 200,
            )
        }

        val result = repo(fakeApi).deleteSavedCard(id = "ref_1")

        assertIs<ApiResponse.Success<RecommendedInstrumentsResponse>>(result)
        assertEquals("deleteSavedCard(ref_1)", fakeApi.callLog.single())
    }

    @Test
    fun `postUPIQrRequest forwards the type`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextUpiQrPostResponse = ApiResponse.Success(postResponse(), responseCode = 200)
        }

        val result = repo(fakeApi).postUPIQrRequest(type = "upi/qr")

        assertIs<ApiResponse.Success<PaymentMethodPostResponse>>(result)
        assertEquals("upiQrPostRequest(upi/qr)", fakeApi.callLog.single())
    }
}
