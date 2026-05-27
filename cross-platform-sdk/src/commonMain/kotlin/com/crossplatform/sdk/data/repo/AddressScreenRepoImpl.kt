package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.implementation.ApiServiceImpl
import com.crossplatform.sdk.data.model.FetchSavedAddress
import com.crossplatform.sdk.data.service.ApiService
import com.crossplatform.sdk.domain.repo.AddressScreenRepo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

class AddressScreenRepoImpl(
    private val apiService: ApiService = ApiServiceImpl(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO // Injectable dispatcher
) : AddressScreenRepo {
    override suspend fun getSavedAddress(): ApiResponse<List<FetchSavedAddress>> = withContext(ioDispatcher) {
        apiService.getSavedAddress()
    }

    override suspend fun deleteSavedAddress(addressRef: String): ApiResponse<FetchSavedAddress> = withContext(ioDispatcher) {
        apiService.deleteSavedAddress(addressRef = addressRef)
    }

}