package com.crossplatform.sdk.data.endpoint

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.ServiceRequest
import com.crossplatform.sdk.data.executeWithResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.handler.UserDataHandler
import com.crossplatform.sdk.data.model.AnalyticsResponse
import com.crossplatform.sdk.data.model.CheckoutDetails
import com.crossplatform.sdk.data.model.FetchCardDetails
import com.crossplatform.sdk.data.model.FetchSavedAddress
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.MethodInstrumentDetails
import com.crossplatform.sdk.data.model.MethodsPostRequest
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.UserDetails
import com.crossplatform.sdk.data.model.requestBody.AnalyticsRequest
import com.crossplatform.sdk.data.model.requestBody.CardDetails
import com.crossplatform.sdk.data.model.requestBody.CardPostRequestBody
import com.crossplatform.sdk.data.model.requestBody.EventAttrs
import com.crossplatform.sdk.data.model.requestBody.InstrumentDetails
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

class ApiServiceImplementation : ApiService {

    private val client get() = ServiceRequest.buildClient()
    private val plainClient get() = ServiceRequest.buildPlainClient()

    private val checkoutDetails: CheckoutDetails
        get() = CheckoutDetailsHandler.checkoutDetails

    private val userData : UserDetails
        get() = UserDataHandler.userData

    override suspend fun getSessionDetails(): ApiResponse<CheckoutDetails> {
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
            eventAttrs = EventAttrs(
                errorMessage = message,
                screenName = screenName
            ),
            deviceDetails = getDeviceDetails()
        )
        return executeWithResponse {
            plainClient.post(
                urlString = "${getEndpoint(checkoutDetails.env)}/ui-analytics",
            ) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
        }
    }

    override suspend fun cardPostRequest(
        cardNumber: String,
        cvv: String,
        cardName: String,
        expiry: String,
        nickName: String?,
        isSaveInstrumentCheckboxClicked: Boolean,
        isSICheckboxClicked: Boolean
    ): ApiResponse<PaymentMethodPostResponse> {
        val requestBody = CardPostRequestBody(
            browserData = getBrowserData(),
            instrumentDetails = InstrumentDetails(
                type = "card/plain",
                card = CardDetails(
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
            client.get(urlString = "/bank-identification-numbers/${cardNumber}")
        }
    }

    override suspend fun deleteSavedAddress(addressRef: String): ApiResponse<FetchSavedAddress> {
        return executeWithResponse {
            client.delete(urlString = "/shoppers/${userData.uniqueId}/addresses/${addressRef}")
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

    override suspend fun fetchPaymentMethods(): ApiResponse<PaymentMethodPostResponse> {
        return executeWithResponse {
            client.get(urlString = "/payment-methods")
        }
    }

    override suspend fun fetchStatus(): ApiResponse<FetchStatusResponse> {
        return executeWithResponse {
            client.get(urlString = "/status")
        }
    }
}