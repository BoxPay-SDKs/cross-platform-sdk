package com.crossplatform.sdk.domain.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.FetchSavedAddress

interface AddressScreenRepo {
    suspend fun getSavedAddress() : ApiResponse<List<FetchSavedAddress>>

    suspend fun deleteSavedAddress(addressRef : String) : ApiResponse<FetchSavedAddress>
}