package com.crossplatform.sdk.fakes

import com.crossplatform.sdk.domain.repo.AddressScreenRepo
import com.crossplatform.sdk.domain.repo.CallUIAnalyticsRepo
import com.crossplatform.sdk.domain.repo.CardScreenRepo
import com.crossplatform.sdk.domain.repo.FetchStatusRepo
import com.crossplatform.sdk.domain.repo.InstantOfferRepo
import com.crossplatform.sdk.domain.repo.MainScreenRepo
import com.crossplatform.sdk.domain.repo.OtherPaymentMethodRepo
import com.crossplatform.sdk.presentation.viewmodel.AddressScreenViewModel
import com.crossplatform.sdk.presentation.viewmodel.BNPLViewModel
import com.crossplatform.sdk.presentation.viewmodel.BoxPayElementsViewModel
import com.crossplatform.sdk.presentation.viewmodel.CardScreenViewModel
import com.crossplatform.sdk.presentation.viewmodel.EMIScreenViewModel
import com.crossplatform.sdk.presentation.viewmodel.InstantOfferViewModel
import com.crossplatform.sdk.presentation.viewmodel.MainScreenViewModel
import com.crossplatform.sdk.presentation.viewmodel.NetBankingViewModel
import com.crossplatform.sdk.presentation.viewmodel.UpiTimerViewModel
import com.crossplatform.sdk.presentation.viewmodel.WalletViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Every real screen (`CardScreen`, `EMIScreen`, `NetBankingScreen`,
 * `WalletScreen`, `BNPLScreen`, `SavedAddressScreen`, `InstantOfferScreen`,
 * `UpiTimerScreen`) resolves its ViewModel via `koinViewModel()` internally
 * — it is *not* passed in as a parameter — so there is no way to render the
 * real screen composable in a test without Koin actually running.
 *
 * This module is a copy of `di/AppModule.kt`'s shape, with every repo
 * interface bound to a controllable fake instead of the real
 * `ApiServiceImpl`-backed implementation. Pass in the specific fake
 * instances you want to configure before rendering; anything you don't pass
 * gets a fresh, unconfigured fake (calling any of its methods will throw,
 * which is usually what you want — it means the screen made a call the test
 * didn't expect).
 *
 * Usage in a test:
 * ```
 * private val cardScreenRepo = FakeCardScreenRepo()
 * @Before fun setUp() { startKoin { modules(testKoinModule(cardScreenRepo = cardScreenRepo)) } }
 * @After fun tearDown() { stopKoin() }
 * ```
 */
internal fun testKoinModule(
    cardScreenRepo: FakeCardScreenRepo = FakeCardScreenRepo(),
    fetchStatusRepo: FakeFetchStatusRepo = FakeFetchStatusRepo(),
    analyticsRepo: FakeCallUIAnalyticsRepo = FakeCallUIAnalyticsRepo(),
    otherPaymentMethodRepo: FakeOtherPaymentMethodRepo = FakeOtherPaymentMethodRepo(),
    mainScreenRepo: FakeMainScreenRepo = FakeMainScreenRepo(),
    addressScreenRepo: FakeAddressScreenRepo = FakeAddressScreenRepo(),
    instantOfferRepo: FakeInstantOfferRepo = FakeInstantOfferRepo(),
) = module {
    single<CardScreenRepo> { cardScreenRepo }
    single<FetchStatusRepo> { fetchStatusRepo }
    single<CallUIAnalyticsRepo> { analyticsRepo }
    single<OtherPaymentMethodRepo> { otherPaymentMethodRepo }
    single<MainScreenRepo> { mainScreenRepo }
    single<AddressScreenRepo> { addressScreenRepo }
    single<InstantOfferRepo> { instantOfferRepo }

    viewModel { MainScreenViewModel(repo = get(), analyticsRepo = get(), otherPaymentMethodRepo = get(), instantOfferRepo = get(), fetchStatusRepo = get()) }
    viewModel { BoxPayElementsViewModel(repo = get(), analyticsRepo = get(), otherPaymentMethodRepo = get(), cardRepo = get(), fetchStatusRepo = get()) }
    viewModel { InstantOfferViewModel(repo = get(), analyticsRepo = get()) }
    viewModel { AddressScreenViewModel(repo = get(), analyticsRepo = get()) }
    viewModel { CardScreenViewModel(repo = get(), fetchStatusRepo = get(), analyticsRepo = get()) }
    viewModel { EMIScreenViewModel(repo = get(), analyticsRepo = get(), cardScreenRepo = get(), fetchStatusRepo = get()) }
    viewModel { WalletViewModel(repo = get(), analyticsRepo = get(), fetchStatusRepo = get()) }
    viewModel { NetBankingViewModel(repo = get(), analyticsRepo = get(), fetchStatusRepo = get()) }
    viewModel { BNPLViewModel(repo = get(), analyticsRepo = get(), fetchStatusRepo = get()) }
    viewModel { UpiTimerViewModel() }
}
