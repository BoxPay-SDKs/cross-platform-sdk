package com.crossplatform.sdk.domain.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.FetchCardDetails
import com.crossplatform.sdk.data.model.NativeOtpResponse
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse

internal interface CardScreenRepo {
    suspend fun getCardDetails(cardNumber: String) : ApiResponse<FetchCardDetails>

    suspend fun postCardDetails(
        type : String,
        cardNumber: String,
        cvv: String,
        cardName: String,
        expiry: String,
        nickName: String?,
        isSaveInstrumentCheckboxClicked: Boolean,
        isSICheckboxClicked: Boolean?,
        surcharges : List<String>?
    ) : ApiResponse<PaymentMethodPostResponse>

    suspend fun submitOtp(otp : String, transactionId : String,isTestEnv : Boolean) : ApiResponse<NativeOtpResponse>

    suspend fun resendOtp(transactionId: String,isTestEnv : Boolean) : ApiResponse<NativeOtpResponse>
}