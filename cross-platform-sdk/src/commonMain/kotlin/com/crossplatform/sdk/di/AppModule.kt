package com.crossplatform.sdk.di

import com.crossplatform.sdk.data.implementation.ApiServiceImpl
import com.crossplatform.sdk.data.repo.AddressScreenRepoImpl
import com.crossplatform.sdk.data.repo.CallUIAnalyticsRepoImpl
import com.crossplatform.sdk.data.repo.CardScreenRepoImpl
import com.crossplatform.sdk.data.repo.EMIScreenRepoImpl
import com.crossplatform.sdk.data.repo.FetchStatusRepoImpl
import com.crossplatform.sdk.data.repo.InstantOfferRepoImpl
import com.crossplatform.sdk.data.repo.MainScreenRepoImpl
import com.crossplatform.sdk.data.repo.OtherPaymentMethodRepoImpl
import com.crossplatform.sdk.data.service.ApiService
import com.crossplatform.sdk.domain.repo.AddressScreenRepo
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import com.crossplatform.sdk.domain.repo.CardScreenRepo
import com.crossplatform.sdk.domain.repo.EMIScreenRepo
import com.crossplatform.sdk.domain.repo.FetchStatusRepo
import com.crossplatform.sdk.domain.repo.InstantOfferRepo
import com.crossplatform.sdk.domain.repo.MainScreenRepo
import com.crossplatform.sdk.domain.repo.OtherPaymentMethodRepo
import com.crossplatform.sdk.presentation.viewmodel.AddressScreenViewModel
import com.crossplatform.sdk.presentation.viewmodel.BNPLViewModel
import com.crossplatform.sdk.presentation.viewmodel.CardScreenViewModel
import com.crossplatform.sdk.presentation.viewmodel.EMIScreenViewModel
import com.crossplatform.sdk.presentation.viewmodel.InstantOfferViewModel
import com.crossplatform.sdk.presentation.viewmodel.MainScreenViewModel
import com.crossplatform.sdk.presentation.viewmodel.NetBankingViewModel
import com.crossplatform.sdk.presentation.viewmodel.OtherPaymentMethodViewModel
import com.crossplatform.sdk.presentation.viewmodel.UpiTimerViewModel
import com.crossplatform.sdk.presentation.viewmodel.WalletViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ✅ Endpoint
    single<ApiService> { ApiServiceImpl() }

    // ✅ bind interface to impl
    single<MainScreenRepo> { MainScreenRepoImpl(apiService = get()) }
    single<OtherPaymentMethodRepo> { OtherPaymentMethodRepoImpl(apiService = get()) }
    single<AddressScreenRepo>{ AddressScreenRepoImpl(apiService = get()) }
    single<CardScreenRepo> { CardScreenRepoImpl(apiService = get()) }
    single<EMIScreenRepo> { EMIScreenRepoImpl(apiService = get()) }
    single<InstantOfferRepo> { InstantOfferRepoImpl(apiService = get()) }
    single<CallUIAnalyticsRepo> { CallUIAnalyticsRepoImpl(apiService = get()) }
    single<FetchStatusRepo> { FetchStatusRepoImpl(apiService = get()) }

    // ✅ Repo
    factory { MainScreenRepoImpl(apiService = get()) }
    factory { OtherPaymentMethodRepoImpl(apiService = get()) }
    factory { AddressScreenRepoImpl(apiService = get()) }
    factory { CardScreenRepoImpl(apiService = get()) }
    factory { EMIScreenRepoImpl(apiService = get()) }
    factory { InstantOfferRepoImpl(apiService = get()) }
    factory { CallUIAnalyticsRepoImpl(apiService = get()) }
    factory { FetchStatusRepoImpl(apiService = get()) }

    // ✅ ViewModels
    viewModel { MainScreenViewModel(repo = get()) }
    viewModel { OtherPaymentMethodViewModel(repo = get()) }
    viewModel { InstantOfferViewModel(repo = get()) }
    viewModel { AddressScreenViewModel(repo = get()) }
    viewModel { CardScreenViewModel(repo = get(), fetchStatusRepo = get()) }
    viewModel { EMIScreenViewModel(repo = get()) }
    viewModel { WalletViewModel(repo = get()) }
    viewModel { NetBankingViewModel(repo = get()) }
    viewModel { BNPLViewModel(repo = get()) }
    viewModel { UpiTimerViewModel(repo = get()) }

}