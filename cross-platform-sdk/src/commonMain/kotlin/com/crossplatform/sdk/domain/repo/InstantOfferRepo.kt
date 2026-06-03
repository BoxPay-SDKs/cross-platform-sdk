package com.crossplatform.sdk.domain.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.AppliedOfferResponse
import com.crossplatform.sdk.data.model.InstantOfferResponse

interface InstantOfferRepo  {
    suspend fun getOffers(
        minAmount: Double,
        maxAmount : Double
    ) : ApiResponse<List<InstantOfferResponse>>
    suspend fun applyOffer(
        offerId : List<String>,
        minAmount : Double
    ) : ApiResponse<AppliedOfferResponse>
}