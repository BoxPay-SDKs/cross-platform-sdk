package com.crossplatform.sdk.domain.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse

interface OtherPaymentMethodRepo {
    suspend fun getPaymentMethods(
        amount: Double?,
        offerId: String?
    ) : ApiResponse<List<PaymentMethod>>

    suspend fun initiatePayment(instrumentDetails: String, paymentType : String, token : String) : ApiResponse<PaymentMethodPostResponse>

    suspend fun initiateEMIPayment(
        cardNumber: String,
        expiryDate: String,
        cvv: String,
        holderName: String,
        cardType: String?,
        offerCode: String?,
        duration: Int?,
        provider : String?
    ) : ApiResponse<PaymentMethodPostResponse>
}