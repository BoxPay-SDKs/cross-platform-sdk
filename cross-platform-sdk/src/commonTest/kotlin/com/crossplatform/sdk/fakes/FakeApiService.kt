package com.crossplatform.sdk.fakes

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.AnalyticsResponse
import com.crossplatform.sdk.data.model.AppliedOfferResponse
import com.crossplatform.sdk.data.model.FetchCardDetails
import com.crossplatform.sdk.data.model.FetchSavedAddress
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.FetchSurchargeResponse
import com.crossplatform.sdk.data.model.InstantOfferResponse
import com.crossplatform.sdk.data.model.NativeOtpResponse
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
    var nextDeleteSavedCard: ApiResponse<List<RecommendedInstrumentsResponse>>? = null
    var nextOffer: ApiResponse<List<InstantOfferResponse>>? = null
    var nextApplyOffer: ApiResponse<AppliedOfferResponse>? = null
    var nextAutoRetryInitiatePayment: ApiResponse<PaymentMethodPostResponse>? = null
    var nextSubmitNativeOtp: ApiResponse<NativeOtpResponse>? = null
    var nextResendOtp: ApiResponse<NativeOtpResponse>? = null
    var nextCancelTransaction: ApiResponse<PaymentMethodPostResponse>? = null

    // Captured arguments for assertion in integration tests
    var lastCardPostSurcharges: List<String>? = null
    var lastEmiPostSurcharges: List<String>? = null
    var lastMethodsPostSurcharges: List<String>? = null
    var lastUpiIntentSurcharges: List<String>? = null
    var lastUpiCollectSurcharges: List<String>? = null
    var lastUpiQrSurcharges: List<String>? = null
    var lastDeleteSavedCardId: String? = null
    var lastDeleteSavedCardType: String? = null
    var lastFetchCardDetailsNumber: String? = null
    var lastUpiCollectVpa: String? = null
    var lastAutoRetryTransactionId: String? = null

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
        return nextAnalyticsResponse ?: ApiResponse.Loading
    }

    override suspend fun cardPostRequest(
        type: String,
        cardNumber: String,
        cvv: String,
        cardName: String,
        expiry: String,
        nickName: String?,
        isSaveInstrumentCheckboxClicked: Boolean,
        isSICheckboxClicked: Boolean?,
        surcharges: List<String>?
    ): ApiResponse<PaymentMethodPostResponse> {
        lastCardPostSurcharges = surcharges
        callLog += "cardPostRequest(type=$type, cardNumber=$cardNumber, expiry=$expiry, surcharges=$surcharges)"
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
        provider: String?,
        surcharges: List<String>?
    ): ApiResponse<PaymentMethodPostResponse> {
        lastEmiPostSurcharges = surcharges
        callLog += "emiPostRequest(cardNumber=$cardNumber, duration=$duration, surcharges=$surcharges)"
        return require(nextEmiPostResponse, "nextEmiPostResponse")
    }

    override suspend fun fetchCardDetails(cardNumber: String): ApiResponse<FetchCardDetails> {
        lastFetchCardDetailsNumber = cardNumber
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
        paymentType: String,
        surcharges: List<String>?
    ): ApiResponse<PaymentMethodPostResponse> {
        lastMethodsPostSurcharges = surcharges
        callLog += "methodsPostRequest($instrumentDetails, $paymentType, surcharges=$surcharges)"
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
        upiApp: String,
        surcharges: List<String>?
    ): ApiResponse<PaymentMethodPostResponse> {
        lastUpiIntentSurcharges = surcharges
        callLog += "upiIntentPostRequest($type, $upiApp, surcharges=$surcharges)"
        return require(nextUpiIntentPostResponse, "nextUpiIntentPostResponse")
    }

    override suspend fun upiCollectPostRequest(
        type: String,
        instrumentRef: String?,
        shopperVpa: String?,
        saveInstrument: Boolean,
        surcharges: List<String>?
    ): ApiResponse<PaymentMethodPostResponse> {
        lastUpiCollectVpa = shopperVpa
        lastUpiCollectSurcharges = surcharges
        callLog += "upiCollectPostRequest($type, vpa=$shopperVpa, surcharges=$surcharges)"
        return require(nextUpiCollectPostResponse, "nextUpiCollectPostResponse")
    }

    override suspend fun upiQrPostRequest(
        type: String,
        surcharges: List<String>?
    ): ApiResponse<PaymentMethodPostResponse> {
        lastUpiQrSurcharges = surcharges
        callLog += "upiQrPostRequest($type, surcharges=$surcharges)"
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

    override suspend fun deleteSavedCard(
        id: String,
        type: String
    ): ApiResponse<List<RecommendedInstrumentsResponse>> {
        lastDeleteSavedCardId = id
        lastDeleteSavedCardType = type
        callLog += "deleteSavedCard(id=$id, type=$type)"
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

    override suspend fun autoRetryInitiatePayment(
        transactionId: String
    ): ApiResponse<PaymentMethodPostResponse> {
        lastAutoRetryTransactionId = transactionId
        callLog += "autoRetryInitiatePayment($transactionId)"
        return require(nextAutoRetryInitiatePayment, "nextAutoRetryInitiatePayment")
    }

    override suspend fun submitNativeOtp(
        otp: String,
        transactionId: String,
        isTestEnv: Boolean
    ): ApiResponse<NativeOtpResponse> {
        callLog += "submitNativeOtp(otp=$otp, txn=$transactionId)"
        return require(nextSubmitNativeOtp, "nextSubmitNativeOtp")
    }

    override suspend fun resendOtp(
        transactionId: String,
        isTestEnv: Boolean
    ): ApiResponse<NativeOtpResponse> {
        callLog += "resendOtp(txn=$transactionId)"
        return require(nextResendOtp, "nextResendOtp")
    }

    override suspend fun cancelTransaction(
        transactionId: String
    ): ApiResponse<PaymentMethodPostResponse> {
        callLog += "cancelTransaction(txn=$transactionId)"
        return require(nextCancelTransaction, "nextCancelTransaction")
    }
}
