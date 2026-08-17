package com.crossplatform.sdk.fakes

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.AnalyticsResponse
import com.crossplatform.sdk.data.model.AppliedOfferResponse
import com.crossplatform.sdk.data.model.FetchCardDetails
import com.crossplatform.sdk.data.model.FetchSavedAddress
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.FetchSurchargeResponse
import com.crossplatform.sdk.data.model.InstantOfferResponse
import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.RecommendedInstrumentsResponse
import com.crossplatform.sdk.data.model.SessionDetails
import com.crossplatform.sdk.data.service.ApiService

/**
 * Test double for [ApiService].
 *
 * Every response is configurable via the `next*` fields so a test can stub
 * exactly what it needs and leave everything else untouched. Any call whose
 * response wasn't stubbed throws, so an unexpected call fails the test loudly
 * instead of returning a silently wrong default.
 *
 * `callLog` records every invocation (with its arguments) so tests can assert
 * *what* was sent to the network layer, not just what came back.
 */
internal class FakeApiService : ApiService {

    val callLog = mutableListOf<String>()

    var nextSessionDetails: ApiResponse<SessionDetails>? = null
    var nextAnalyticsResponse: ApiResponse<AnalyticsResponse>? = null
    var nextCardPostResponse: ApiResponse<PaymentMethodPostResponse>? = null
    var nextEmiPostResponse: ApiResponse<PaymentMethodPostResponse>? = null
    var nextFetchCardDetails: ApiResponse<FetchCardDetails>? = null
    var nextDeleteSavedAddress: ApiResponse<FetchSavedAddress>? = null
    var nextMethodsPostResponse: ApiResponse<PaymentMethodPostResponse>? = null
    var nextPaymentMethods: ApiResponse<List<PaymentMethod>>? = null
    var nextFetchStatus: ApiResponse<FetchStatusResponse>? = null
    var nextUpiIntentPostResponse: ApiResponse<PaymentMethodPostResponse>? = null
    var nextUpiCollectPostResponse: ApiResponse<PaymentMethodPostResponse>? = null
    var nextUpiQrPostResponse: ApiResponse<PaymentMethodPostResponse>? = null
    var nextSavedCardPostResponse: ApiResponse<PaymentMethodPostResponse>? = null
    var nextRecommendedInstruments: ApiResponse<List<RecommendedInstrumentsResponse>>? = null
    var nextSurcharge: ApiResponse<FetchSurchargeResponse>? = null
    var nextSavedAddress: ApiResponse<List<FetchSavedAddress>>? = null
    var nextDeleteSavedCard: ApiResponse<RecommendedInstrumentsResponse>? = null
    var nextOffer: ApiResponse<List<InstantOfferResponse>>? = null
    var nextApplyOffer: ApiResponse<AppliedOfferResponse>? = null
    var nextAutoRetryInitiatePayment: ApiResponse<PaymentMethodPostResponse>? = null

    private fun <T> require(value: T?, name: String): T =
        value ?: error("FakeApiService.$name was called but no stub was configured")

    override suspend fun getSessionDetails(): ApiResponse<SessionDetails> {
        callLog += "getSessionDetails()"
        return require(nextSessionDetails, "nextSessionDetails")
    }

    override suspend fun callUiAnalytics(
        uiEvent: String,
        screenName: String,
        message: String
    ): ApiResponse<AnalyticsResponse> {
        callLog += "callUiAnalytics($uiEvent, $screenName, $message)"
        return require(nextAnalyticsResponse, "nextAnalyticsResponse")
    }

    override suspend fun cardPostRequest(
        type: String,
        cardNumber: String,
        cvv: String,
        cardName: String,
        expiry: String,
        nickName: String?,
        isSaveInstrumentCheckboxClicked: Boolean,
        isSICheckboxClicked: Boolean?
    ): ApiResponse<PaymentMethodPostResponse> {
        callLog += "cardPostRequest(type=$type, cardNumber=$cardNumber, expiry=$expiry)"
        return require(nextCardPostResponse, "nextCardPostResponse")
    }

    override suspend fun emiPostRequest(
        cardNumber: String,
        expiryDate: String,
        cvv: String,
        holderName: String,
        cardType: String?,
        offerCode: String?,
        duration: Int?,
        provider: String?
    ): ApiResponse<PaymentMethodPostResponse> {
        callLog += "emiPostRequest(cardNumber=$cardNumber, duration=$duration)"
        return require(nextEmiPostResponse, "nextEmiPostResponse")
    }

    override suspend fun fetchCardDetails(cardNumber: String): ApiResponse<FetchCardDetails> {
        callLog += "fetchCardDetails($cardNumber)"
        return require(nextFetchCardDetails, "nextFetchCardDetails")
    }

    override suspend fun deleteSavedAddress(addressRef: String): ApiResponse<FetchSavedAddress> {
        callLog += "deleteSavedAddress($addressRef)"
        return require(nextDeleteSavedAddress, "nextDeleteSavedAddress")
    }

    override suspend fun methodsPostRequest(
        instrumentDetails: String,
        token: String,
        paymentType: String
    ): ApiResponse<PaymentMethodPostResponse> {
        callLog += "methodsPostRequest($instrumentDetails, $paymentType)"
        return require(nextMethodsPostResponse, "nextMethodsPostResponse")
    }

    override suspend fun fetchPaymentMethods(
        amount: Double?,
        offerId: String?
    ): ApiResponse<List<PaymentMethod>> {
        callLog += "fetchPaymentMethods(amount=$amount, offerId=$offerId)"
        return require(nextPaymentMethods, "nextPaymentMethods")
    }

    override suspend fun fetchStatus(): ApiResponse<FetchStatusResponse> {
        callLog += "fetchStatus()"
        return require(nextFetchStatus, "nextFetchStatus")
    }

    override suspend fun upiIntentPostRequest(
        type: String,
        upiApp: String
    ): ApiResponse<PaymentMethodPostResponse> {
        callLog += "upiIntentPostRequest($type, $upiApp)"
        return require(nextUpiIntentPostResponse, "nextUpiIntentPostResponse")
    }

    override suspend fun upiCollectPostRequest(
        type: String,
        instrumentRef: String?,
        shopperVpa: String?,
        saveInstrument: Boolean?
    ): ApiResponse<PaymentMethodPostResponse> {
        callLog += "upiCollectPostRequest($type, $shopperVpa)"
        return require(nextUpiCollectPostResponse, "nextUpiCollectPostResponse")
    }

    override suspend fun upiQrPostRequest(type: String): ApiResponse<PaymentMethodPostResponse> {
        callLog += "upiQrPostRequest($type)"
        return require(nextUpiQrPostResponse, "nextUpiQrPostResponse")
    }

    override suspend fun savedCardPostRequest(
        instrumentRef: String,
        isSICheckboxClicked: Boolean?
    ): ApiResponse<PaymentMethodPostResponse> {
        callLog += "savedCardPostRequest($instrumentRef)"
        return require(nextSavedCardPostResponse, "nextSavedCardPostResponse")
    }

    override suspend fun getRecommendedInstruments(): ApiResponse<List<RecommendedInstrumentsResponse>> {
        callLog += "getRecommendedInstruments()"
        return require(nextRecommendedInstruments, "nextRecommendedInstruments")
    }

    override suspend fun getSurcharge(
        amount: Double,
        currencyCode: String
    ): ApiResponse<FetchSurchargeResponse> {
        callLog += "getSurcharge(amount=$amount, currencyCode=$currencyCode)"
        return require(nextSurcharge, "nextSurcharge")
    }

    override suspend fun getSavedAddress(): ApiResponse<List<FetchSavedAddress>> {
        callLog += "getSavedAddress()"
        return require(nextSavedAddress, "nextSavedAddress")
    }

    override suspend fun deleteSavedCard(id: String): ApiResponse<RecommendedInstrumentsResponse> {
        callLog += "deleteSavedCard($id)"
        return require(nextDeleteSavedCard, "nextDeleteSavedCard")
    }

    override suspend fun getOffer(
        minAmount: Double,
        maxAmount: Double
    ): ApiResponse<List<InstantOfferResponse>> {
        callLog += "getOffer(min=$minAmount, max=$maxAmount)"
        return require(nextOffer, "nextOffer")
    }

    override suspend fun applyOffer(
        offerId: List<String>,
        minAmount: Double
    ): ApiResponse<AppliedOfferResponse> {
        callLog += "applyOffer($offerId, min=$minAmount)"
        return require(nextApplyOffer, "nextApplyOffer")
    }

    override suspend fun autoRetryInitiatePayment(transactionId: String): ApiResponse<PaymentMethodPostResponse> {
        callLog += "autoRetryInitiatePayment($transactionId)"
        return require(nextAutoRetryInitiatePayment, "nextAutoRetryInitiatePayment")
    }
}
