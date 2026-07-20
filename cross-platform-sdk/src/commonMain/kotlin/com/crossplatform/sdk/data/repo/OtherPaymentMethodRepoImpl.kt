package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.implementation.ApiServiceImpl
import com.crossplatform.sdk.data.model.FetchStatusResponse
import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.service.ApiService
import com.crossplatform.sdk.domain.repo.OtherPaymentMethodRepo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class OtherPaymentMethodRepoImpl(
    private val apiService: ApiService = ApiServiceImpl(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO // Injectable dispatcher
) : OtherPaymentMethodRepo {

    override suspend fun getPaymentMethods(
        amount: Double?,
        offerId: String?
    ): ApiResponse<List<PaymentMethod>> = withContext(ioDispatcher) {
        apiService.fetchPaymentMethods(
            amount = amount,
            offerId = offerId
        )
    }

    override suspend fun initiatePayment(instrumentDetails: String): ApiResponse<PaymentMethodPostResponse> = withContext(ioDispatcher) {
        apiService.methodsPostRequest(instrumentDetails)
    }

    override suspend fun initiateEMIPayment(
        cardNumber: String,
        expiryDate: String,
        cvv: String,
        holderName: String,
        cardType: String?,
        offerCode: String?,
        duration: Int?,
        provider: String?
    ): ApiResponse<PaymentMethodPostResponse> = withContext(ioDispatcher) {
        apiService.emiPostRequest(
            cardNumber = cardNumber,
            expiryDate = expiryDate,
            cvv = cvv,
            holderName = holderName,
            cardType = cardType,
            offerCode = offerCode,
            duration = duration,
            provider = provider
        )
    }

}