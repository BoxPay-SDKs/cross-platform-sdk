package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.implementation.ApiServiceImpl
import com.crossplatform.sdk.data.service.ApiService
import com.crossplatform.sdk.domain.repo.InstantOfferRepo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

class InstantOfferRepoImpl(
    private val apiService: ApiService = ApiServiceImpl(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO // Injectable dispatcher
) : InstantOfferRepo {

}