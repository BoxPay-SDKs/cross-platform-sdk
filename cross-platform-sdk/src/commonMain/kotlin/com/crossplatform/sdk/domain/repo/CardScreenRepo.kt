package com.crossplatform.sdk.domain.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.FetchCardDetails
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse

interface CardScreenRepo {
    suspend fun getCardDetails(cardNumber: String) : ApiResponse<FetchCardDetails>

    suspend fun postCardDetails(
        type : String,
        cardNumber: String,
        cvv: String,
        cardName: String,
        expiry: String,
        nickName: String?,
        isSaveInstrumentCheckboxClicked: Boolean,
        isSICheckboxClicked: Boolean?
    ) : ApiResponse<PaymentMethodPostResponse>
}