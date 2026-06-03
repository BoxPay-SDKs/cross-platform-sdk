package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.implementation.ApiServiceImpl
import com.crossplatform.sdk.data.model.AppliedOfferResponse
import com.crossplatform.sdk.data.model.InstantOfferResponse
import com.crossplatform.sdk.data.service.ApiService
import com.crossplatform.sdk.domain.repo.InstantOfferRepo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class InstantOfferRepoImpl(
    private val apiService: ApiService = ApiServiceImpl(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO // Injectable dispatcher
) : InstantOfferRepo {
    override suspend fun getOffers(
        minAmount: Double,
        maxAmount: Double
    ): ApiResponse<List<InstantOfferResponse>> = withContext(ioDispatcher) {
        apiService.getOffer(
            minAmount = minAmount,
            maxAmount = maxAmount
        )
    }

    override suspend fun applyOffer(
        offerId: List<String>,
        minAmount: Double
    ): ApiResponse<AppliedOfferResponse> = withContext(ioDispatcher) {
        apiService.applyOffer(
            offerId = offerId,
            minAmount = minAmount
        )
    }

}