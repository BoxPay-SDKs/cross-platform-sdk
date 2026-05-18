package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.implementation.ApiServiceImpl
import com.crossplatform.sdk.data.service.ApiService
import com.crossplatform.sdk.domain.repo.EMIScreenRepo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

class EMIScreenRepoImpl (
    private val apiService: ApiService = ApiServiceImpl(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO // Injectable dispatcher
) : EMIScreenRepo {

}