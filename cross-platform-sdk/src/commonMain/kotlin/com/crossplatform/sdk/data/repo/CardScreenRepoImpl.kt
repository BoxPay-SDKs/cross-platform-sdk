package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.implementation.ApiServiceImpl
import com.crossplatform.sdk.data.model.FetchCardDetails
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.service.ApiService
import com.crossplatform.sdk.domain.repo.CardScreenRepo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class CardScreenRepoImpl(
    private val apiService: ApiService = ApiServiceImpl(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO // Injectable dispatcher
) : CardScreenRepo {
    override suspend fun getCardDetails(cardNumber: String): ApiResponse<FetchCardDetails> =
        withContext(ioDispatcher) {
            apiService.fetchCardDetails(cardNumber = cardNumber)
        }

    override suspend fun postCardDetails(
        type : String,
        cardNumber: String,
        cvv: String,
        cardName: String,
        expiry: String,
        nickName: String?,
        isSaveInstrumentCheckboxClicked: Boolean,
        isSICheckboxClicked: Boolean?
    ): ApiResponse<PaymentMethodPostResponse> = withContext(ioDispatcher) {
        apiService.cardPostRequest(
            type = type,
            cardNumber = cardNumber,
            cvv = cvv,
            cardName = cardName,
            expiry = expiry,
            nickName = nickName,
            isSaveInstrumentCheckboxClicked = isSaveInstrumentCheckboxClicked,
            isSICheckboxClicked = isSICheckboxClicked
        )
    }
}