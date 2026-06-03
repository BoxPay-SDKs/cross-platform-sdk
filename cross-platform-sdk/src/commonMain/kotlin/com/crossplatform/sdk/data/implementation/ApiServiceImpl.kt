package com.crossplatform.sdk.data.implementation

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.ServiceRequest
import com.crossplatform.sdk.data.executeWithResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.handler.UserDataHandler
import com.crossplatform.sdk.data.model.AnalyticsResponse
import com.crossplatform.sdk.data.model.AppliedOfferResponse
import com.crossplatform.sdk.data.model.CheckoutDetails
import com.crossplatform.sdk.data.model.FetchCardDetails
import com.crossplatform.sdk.data.model.FetchSavedAddress
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.FetchSurchargeResponse
import com.crossplatform.sdk.data.model.InstantOfferResponse
import com.crossplatform.sdk.data.model.MethodInstrumentDetails
import com.crossplatform.sdk.data.model.MethodsPostRequest
import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.RecommendedInstrumentsResponse
import com.crossplatform.sdk.data.model.SessionDetails
import com.crossplatform.sdk.data.model.UserDetails
import com.crossplatform.sdk.data.model.requestBody.AnalyticsRequest
import com.crossplatform.sdk.data.model.requestBody.ApplyOfferRequestBody
import com.crossplatform.sdk.data.model.requestBody.CardPostRequestBody
import com.crossplatform.sdk.data.model.requestBody.InstantOfferRequestBody
import com.crossplatform.sdk.data.model.requestBody.SavedCardPostRequestBody
import com.crossplatform.sdk.data.model.requestBody.SurchargeRequestBody
import com.crossplatform.sdk.data.model.requestBody.UPICollectRequestBody
import com.crossplatform.sdk.data.model.requestBody.UPIIntentRequestBody
import com.crossplatform.sdk.data.model.requestBody.UPIQRRequestBody
import com.crossplatform.sdk.data.service.ApiService
import com.crossplatform.sdk.presentation.getBrowserData
import com.crossplatform.sdk.presentation.getDeviceDetails
import com.crossplatform.sdk.presentation.getEndpoint
import com.crossplatform.sdk.presentation.getShopperDetails
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ApiServiceImpl : ApiService {

    private val client get() = ServiceRequest.buildClient()
    private val plainClient get() = ServiceRequest.buildPlainClient()

    private val checkoutDetails: CheckoutDetails
        get() = CheckoutDetailsHandler.checkoutDetails

    private val userData : UserDetails
        get() = UserDataHandler.userData

    override suspend fun getSessionDetails(): ApiResponse<SessionDetails> {
        return executeWithResponse {
            client.get("")
        }
    }

    override suspend fun callUiAnalytics(
        uiEvent: String,
        screenName: String,
        message: String
    ): ApiResponse<AnalyticsResponse> {
        val requestBody = AnalyticsRequest(
            browserData = getBrowserData(),
            callerToken = checkoutDetails.token,
            uiEvent = uiEvent,
            eventAttrs = AnalyticsRequest.EventAttrs(
                errorMessage = message,
                screenName = screenName
            ),
            deviceDetails = getDeviceDetails()
        )
        return executeWithResponse {
            plainClient.post(
                urlString = "${getEndpoint(checkoutDetails.isTestEnv)}/ui-analytics",
            ) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
        }
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
        val requestBody = CardPostRequestBody(
            browserData = getBrowserData(),
            instrumentDetails = CardPostRequestBody.InstrumentDetails(
                type = type,
                card = CardPostRequestBody.CardDetails(
                    number = cardNumber,
                    expiry = expiry,
                    cvc = cvv,
                    holderName = cardName,
                    nickName = nickName
                ),
                saveInstrument = isSaveInstrumentCheckboxClicked
            ),
            shopper = getShopperDetails(),
            deviceDetails = getDeviceDetails(),
            oneTimePayment = isSICheckboxClicked
        )
        return executeWithResponse {
            client.post(urlString = "") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
        }
    }

    override suspend fun fetchCardDetails(cardNumber: String): ApiResponse<FetchCardDetails> {
        return executeWithResponse {
            client.post(urlString = "bank-identification-numbers/${cardNumber}")
        }
    }

    override suspend fun deleteSavedAddress(addressRef: String): ApiResponse<FetchSavedAddress> {
        return executeWithResponse {
            client.delete(urlString = "shoppers/${userData.uniqueId}/addresses/${addressRef}")
        }
    }

    override suspend fun methodsPostRequest(
        instrumentDetails: String
    ): ApiResponse<PaymentMethodPostResponse> {
        val requestBody = MethodsPostRequest(
            browserData = getBrowserData(),
            instrumentDetails = MethodInstrumentDetails(
                type = instrumentDetails
            ),
            shopper = getShopperDetails(),
            deviceDetails = getDeviceDetails()
        )
        return executeWithResponse {
            client.post(urlString = "") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
        }
    }

    override suspend fun fetchPaymentMethods(
        amount: Double?,
        offerId: String?
    ): ApiResponse<List<PaymentMethod>> {
        return executeWithResponse {
            client.get(urlString = "payment-methods") {
                url {
                    amount?.let { parameters.append("amount", "$it") }
                    offerId?.let { parameters.append("offerId", it) }
                }
            }
        }
    }

    override suspend fun fetchStatus(): ApiResponse<FetchStatusResponse> {
        return executeWithResponse {
            client.get(urlString = "transactions/${checkoutDetails.transactionId}/status")
        }
    }

    override suspend fun upiIntentPostRequest(type: String,upiApp: String): ApiResponse<PaymentMethodPostResponse> {
        val requestBody = UPIIntentRequestBody(
            browserData = getBrowserData(),
            shopper = getShopperDetails(),
            deviceDetails = getDeviceDetails(),
            instrumentDetails = UPIIntentRequestBody.Instrument(
                type = type,
                upiAppDetails = UPIIntentRequestBody.UPIAppDetails(
                    upiApp = upiApp
                )
            )
        )
        return executeWithResponse {
            client.post(urlString = "") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
        }
    }

    override suspend fun upiCollectPostRequest(
        type: String,
        instrumentRef: String?,
        shopperVpa: String?,
        saveInstrument : Boolean?
    ): ApiResponse<PaymentMethodPostResponse> {
        val requestBody = UPICollectRequestBody(
            browserData = getBrowserData(),
            shopper = getShopperDetails(),
            deviceDetails = getDeviceDetails(),
            instrumentDetails = UPICollectRequestBody.Instrument(
                type = type,
                upi = UPICollectRequestBody.UPIDetails(
                    instrumentRef = instrumentRef,
                    shopperVpa = shopperVpa
                ),
                saveInstrument = saveInstrument
            )
        )
        return executeWithResponse {
            client.post(urlString = "") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
        }
    }

    override suspend fun upiQrPostRequest(
        type : String,
    ): ApiResponse<PaymentMethodPostResponse> {
        val requestBody = UPIQRRequestBody(
            browserData = getBrowserData(),
            shopper = getShopperDetails(),
            deviceDetails = getDeviceDetails(),
            instrumentDetails = UPIQRRequestBody.Instrument(
                type = type
            )
        )
        return executeWithResponse {
            client.post(urlString = "") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
        }
    }

    override suspend fun savedCardPostRequest(instrumentRef: String, isSICheckboxClicked: Boolean?): ApiResponse<PaymentMethodPostResponse> {
        val requestBody = SavedCardPostRequestBody(
            browserData = getBrowserData(),
            shopper = getShopperDetails(),
            deviceDetails = getDeviceDetails(),
            instrumentDetails = SavedCardPostRequestBody.Instrument(
                type = "card/token",
                card = SavedCardPostRequestBody.SavedCard(
                    instrumentRef = instrumentRef
                )
            ),
            oneTimePayment = isSICheckboxClicked == false
        )
        return executeWithResponse {
            client.post(urlString = "") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
        }
    }

    override suspend fun getRecommendedInstruments(): ApiResponse<List<RecommendedInstrumentsResponse>> {
        return executeWithResponse {
            client.get(urlString = "shoppers/${userData.uniqueId}/recommended-instruments") {
                contentType(ContentType.Application.Json)
            }
        }
    }

    override suspend fun getSurcharge(
        amount: Double,
        currencyCode: String
    ): ApiResponse<FetchSurchargeResponse> {
        val requestBody = SurchargeRequestBody(
            discountedMoney = SurchargeRequestBody.DiscountedMoney(
                amount = amount,
                currencyCode = currencyCode
            )
        )
        return executeWithResponse {
            client.post(urlString = "surcharges/evaluate") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
        }
    }

    override suspend fun getSavedAddress(): ApiResponse<List<FetchSavedAddress>> {
        return executeWithResponse {
            client.get(urlString = "shoppers/${userData.uniqueId}/addresses")
        }
    }

    override suspend fun deleteSavedCard(id: String): ApiResponse<RecommendedInstrumentsResponse> {
        return executeWithResponse {
            client.delete(urlString = "shoppers/${userData.uniqueId}/instruments/$id")
        }
    }

    override suspend fun getOffer(
        minAmount: Double,
        maxAmount: Double
    ): ApiResponse<List<InstantOfferResponse>> {
        val requestBody = InstantOfferRequestBody(
            minAmount = minAmount,
            maxAmount = maxAmount,
            currencyCode = checkoutDetails.currencyCode
        )
        return executeWithResponse{
            client.post(urlString = "offers/search") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
        }
    }

    override suspend fun applyOffer(
        offerId: List<String>,
        minAmount: Double
    ): ApiResponse<AppliedOfferResponse> {
        val requestBody = ApplyOfferRequestBody(
            offerSearchRequest = ApplyOfferRequestBody.OfferSearchRequest(
                minAmount = minAmount,
                offers = offerId
            ),
        )
        return executeWithResponse{
            client.post(urlString = "offers/evaluate") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
        }
    }
}