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
import com.crossplatform.sdk.domain.repo.AddressScreenRepo
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import com.crossplatform.sdk.domain.repo.CardScreenRepo
import com.crossplatform.sdk.domain.repo.FetchStatusRepo
import com.crossplatform.sdk.domain.repo.InstantOfferRepo
import com.crossplatform.sdk.domain.repo.MainScreenRepo
import com.crossplatform.sdk.domain.repo.OtherPaymentMethodRepo

/**
 * Lightweight, fully-controllable fakes for the repo interfaces consumed by
 * view models. Keeping these as hand-written fakes (rather than a mocking
 * framework) keeps them usable from commonTest, i.e. runnable on every KMP
 * target rather than only the JVM/Android target.
 */

internal class FakeCardScreenRepo : CardScreenRepo {
    var getCardDetailsResult: ApiResponse<FetchCardDetails> = ApiResponse.Loading
    var postCardDetailsResult: ApiResponse<PaymentMethodPostResponse> = ApiResponse.Loading

    var lastGetCardDetailsCardNumber: String? = null
    var postCardDetailsCallCount: Int = 0

    override suspend fun getCardDetails(cardNumber: String): ApiResponse<FetchCardDetails> {
        lastGetCardDetailsCardNumber = cardNumber
        return getCardDetailsResult
    }

    override suspend fun postCardDetails(
        type: String,
        cardNumber: String,
        cvv: String,
        cardName: String,
        expiry: String,
        nickName: String?,
        isSaveInstrumentCheckboxClicked: Boolean,
        isSICheckboxClicked: Boolean?
    ): ApiResponse<PaymentMethodPostResponse> {
        postCardDetailsCallCount++
        return postCardDetailsResult
    }
}

internal class FakeFetchStatusRepo : FetchStatusRepo {
    var fetchStatusResult: ApiResponse<FetchStatusResponse> = ApiResponse.Loading
    var autoRetryResult: ApiResponse<PaymentMethodPostResponse> = ApiResponse.Loading

    var fetchStatusCallCount: Int = 0
    var lastAutoRetryTransactionId: String? = null

    override suspend fun fetchStatus(): ApiResponse<FetchStatusResponse> {
        fetchStatusCallCount++
        return fetchStatusResult
    }

    override suspend fun autoRetryInitiatePayment(transactionId: String): ApiResponse<PaymentMethodPostResponse> {
        lastAutoRetryTransactionId = transactionId
        return autoRetryResult
    }
}

internal class FakeCallUIAnalyticsRepo : CallUIAnalyticsRepo {
    var result: ApiResponse<AnalyticsResponse> = ApiResponse.Loading
    val events = mutableListOf<Triple<String, String, String>>()

    override suspend fun callUiAnalytics(
        uiEvent: String,
        screenName: String,
        message: String
    ): ApiResponse<AnalyticsResponse> {
        events += Triple(uiEvent, screenName, message)
        return result
    }
}

internal class FakeOtherPaymentMethodRepo : OtherPaymentMethodRepo {
    var getPaymentMethodsResult: ApiResponse<List<PaymentMethod>> = ApiResponse.Loading
    var initiatePaymentResult: ApiResponse<PaymentMethodPostResponse> = ApiResponse.Loading
    var initiateEMIPaymentResult: ApiResponse<PaymentMethodPostResponse> = ApiResponse.Loading

    var lastGetPaymentMethodsAmount: Double? = null
    var lastGetPaymentMethodsOfferId: String? = null
    var lastInitiateEMIPaymentDuration: Int? = null

    override suspend fun getPaymentMethods(amount: Double?, offerId: String?): ApiResponse<List<PaymentMethod>> {
        lastGetPaymentMethodsAmount = amount
        lastGetPaymentMethodsOfferId = offerId
        return getPaymentMethodsResult
    }

    override suspend fun initiatePayment(
        instrumentDetails: String,
        paymentType: String,
        token: String
    ): ApiResponse<PaymentMethodPostResponse> = initiatePaymentResult

    override suspend fun initiateEMIPayment(
        cardNumber: String,
        expiryDate: String,
        cvv: String,
        holderName: String,
        cardType: String?,
        offerCode: String?,
        duration: Int?,
        provider: String?
    ): ApiResponse<PaymentMethodPostResponse> {
        lastInitiateEMIPaymentDuration = duration
        return initiateEMIPaymentResult
    }
}

internal class FakeMainScreenRepo : MainScreenRepo {
    var sessionDetailsResult: ApiResponse<SessionDetails> = ApiResponse.Loading
    var upiIntentResult: ApiResponse<PaymentMethodPostResponse> = ApiResponse.Loading
    var upiCollectResult: ApiResponse<PaymentMethodPostResponse> = ApiResponse.Loading
    var recommendedInstrumentsResult: ApiResponse<List<RecommendedInstrumentsResponse>> = ApiResponse.Loading
    var savedCardPostResult: ApiResponse<PaymentMethodPostResponse> = ApiResponse.Loading
    var surchargeResult: ApiResponse<FetchSurchargeResponse> = ApiResponse.Loading
    var deleteSavedCardResult: ApiResponse<RecommendedInstrumentsResponse> = ApiResponse.Loading
    var upiQrResult: ApiResponse<PaymentMethodPostResponse> = ApiResponse.Loading

    var lastUpiCollectVpa: String? = null

    override suspend fun getSessionDetails(): ApiResponse<SessionDetails> = sessionDetailsResult

    override suspend fun postUpiIntentRequest(type: String, upiApp: String): ApiResponse<PaymentMethodPostResponse> =
        upiIntentResult

    override suspend fun postUpiCollectRequest(
        type: String,
        instrumentRef: String?,
        shopperVpa: String?,
        saveInstrument: Boolean?
    ): ApiResponse<PaymentMethodPostResponse> {
        lastUpiCollectVpa = shopperVpa
        return upiCollectResult
    }

    override suspend fun fetchRecommendedInstruments(): ApiResponse<List<RecommendedInstrumentsResponse>> =
        recommendedInstrumentsResult

    override suspend fun postSavedCardRequest(instrumentRef: String, isSICheckboxChecked: Boolean): ApiResponse<PaymentMethodPostResponse> =
        savedCardPostResult

    override suspend fun getSurcharge(amount: Double, currencyCode: String): ApiResponse<FetchSurchargeResponse> =
        surchargeResult

    override suspend fun deleteSavedCard(id: String): ApiResponse<RecommendedInstrumentsResponse> = deleteSavedCardResult

    override suspend fun postUPIQrRequest(type: String): ApiResponse<PaymentMethodPostResponse> = upiQrResult
}

internal class FakeAddressScreenRepo : AddressScreenRepo {
    var savedAddressResult: ApiResponse<List<FetchSavedAddress>> = ApiResponse.Loading
    var deleteSavedAddressResult: ApiResponse<FetchSavedAddress> = ApiResponse.Loading
    var lastDeletedAddressRef: String? = null

    override suspend fun getSavedAddress(): ApiResponse<List<FetchSavedAddress>> = savedAddressResult

    override suspend fun deleteSavedAddress(addressRef: String): ApiResponse<FetchSavedAddress> {
        lastDeletedAddressRef = addressRef
        return deleteSavedAddressResult
    }
}

internal class FakeInstantOfferRepo : InstantOfferRepo {
    var offersResult: ApiResponse<List<InstantOfferResponse>> = ApiResponse.Loading
    var applyOfferResult: ApiResponse<AppliedOfferResponse> = ApiResponse.Loading
    var lastAppliedOfferIds: List<String>? = null

    override suspend fun getOffers(minAmount: Double, maxAmount: Double): ApiResponse<List<InstantOfferResponse>> =
        offersResult

    override suspend fun applyOffer(offerId: List<String>, minAmount: Double): ApiResponse<AppliedOfferResponse> {
        lastAppliedOfferIds = offerId
        return applyOfferResult
    }
}
