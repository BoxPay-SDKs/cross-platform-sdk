package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.Method
import com.crossplatform.sdk.data.model.PaymentActions
import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.data.model.PaymentMethodPostResponse
import com.crossplatform.sdk.data.model.TransactionStatus
import com.crossplatform.sdk.fakes.FakeCallUIAnalyticsRepo
import com.crossplatform.sdk.fakes.FakeFetchStatusRepo
import com.crossplatform.sdk.fakes.FakeOtherPaymentMethodRepo
import com.crossplatform.sdk.presentation.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModelTest {

    private lateinit var repo: FakeOtherPaymentMethodRepo
    private lateinit var analyticsRepo: FakeCallUIAnalyticsRepo
    private lateinit var fetchStatusRepo: FakeFetchStatusRepo

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = FakeOtherPaymentMethodRepo()
        analyticsRepo = FakeCallUIAnalyticsRepo()
        fetchStatusRepo = FakeFetchStatusRepo()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun wallet(title: String) = PaymentMethod(id = title, type = "Wallet", brand = "Wallet", title = title)

    @Test
    fun `on init, the wallet list is filtered to wallet methods and sorted`() = runTest {
        repo.getPaymentMethodsResult = ApiResponse.Success(listOf(wallet("PhonePe"), wallet("Google Pay")), responseCode = 200)

        val viewModel = WalletViewModel(repo, analyticsRepo, fetchStatusRepo)

        assertIs<UiState.Success<*>>(viewModel.uiState.value)
        val names = (viewModel.uiState.value as UiState.Success<List<*>>).data
            .map { (it as com.crossplatform.sdk.domain.model.SelectedPaymentMethod).displayName }
        assertEquals(listOf("Google Pay", "PhonePe"), names)
    }

    @Test
    fun `on init, an error is surfaced as UiState_Error`() = runTest {
        repo.getPaymentMethodsResult = ApiResponse.Error(message = "could not load wallets")

        val viewModel = WalletViewModel(repo, analyticsRepo, fetchStatusRepo)

        assertIs<UiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `onSearch filters the already-loaded wallet list without re-hitting the network`() = runTest {
        repo.getPaymentMethodsResult = ApiResponse.Success(listOf(wallet("PhonePe"), wallet("Google Pay"), wallet("Paytm")), responseCode = 200)
        val viewModel = WalletViewModel(repo, analyticsRepo, fetchStatusRepo)

        viewModel.onSearch("pay")

        val names = (viewModel.uiState.value as UiState.Success<List<*>>).data
            .map { (it as com.crossplatform.sdk.domain.model.SelectedPaymentMethod).displayName }
        assertEquals(listOf("Google Pay", "Paytm"), names)
        assertEquals("pay", viewModel.searchQuery.value)
    }

    @Test
    fun `postWalletRequest with a redirect action stores the url and shows the webview`() = runTest {
        repo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        repo.initiatePaymentResult = ApiResponse.Success(
            PaymentMethodPostResponse(
                transactionId = "txn_1", transactionTimestampLocale = "12 Aug 2026",
                status = TransactionStatus(status = "RequiresAction", reason = "", reasonCode = ""),
                actions = listOf(PaymentActions(type = "redirect", url = "https://wallet.example.com/pay")),
                paymentMethod = Method(type = "Wallet", brand = "Wallet"),
            ),
            responseCode = 200,
        )
        val viewModel = WalletViewModel(repo, analyticsRepo, fetchStatusRepo)

        viewModel.postWalletRequest(instrumentValue = "PhonePe")

        assertEquals("https://wallet.example.com/pay", viewModel.url.value)
        assertTrue(viewModel.showWebview.value)
        assertEquals(1, analyticsRepo.events.size)
    }

    @Test
    fun `postWalletRequest turns off the loading animation on error`() = runTest {
        repo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        repo.initiatePaymentResult = ApiResponse.Error(message = "wallet unavailable")
        val viewModel = WalletViewModel(repo, analyticsRepo, fetchStatusRepo)

        viewModel.postWalletRequest(instrumentValue = "PhonePe")

        assertEquals(false, viewModel.isBoxPayAnimationVisible.value)
    }
}
