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
class BNPLViewModelTest {

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

    private fun provider(title: String) = PaymentMethod(id = title, type = "BuyNowPayLater", brand = "BuyNowPayLater", title = title)

    @Test
    fun `on init, the provider list is filtered to buynowpaylater methods and sorted`() = runTest {
        repo.getPaymentMethodsResult = ApiResponse.Success(listOf(provider("Simpl"), provider("LazyPay")), responseCode = 200)

        val viewModel = BNPLViewModel(repo, fetchStatusRepo, analyticsRepo)

        assertIs<UiState.Success<*>>(viewModel.uiState.value)
        val names = (viewModel.uiState.value as UiState.Success<List<*>>).data
            .map { (it as com.crossplatform.sdk.domain.model.SelectedPaymentMethod).displayName }
        assertEquals(listOf("LazyPay", "Simpl"), names)
    }

    @Test
    fun `on init, an error is surfaced as UiState_Error`() = runTest {
        repo.getPaymentMethodsResult = ApiResponse.Error(message = "could not load BNPL providers")

        val viewModel = BNPLViewModel(repo, fetchStatusRepo, analyticsRepo)

        assertIs<UiState.Error>(viewModel.uiState.value)
    }

    @Test
    fun `onSearch filters the already-loaded provider list`() = runTest {
        repo.getPaymentMethodsResult = ApiResponse.Success(listOf(provider("Simpl"), provider("LazyPay")), responseCode = 200)
        val viewModel = BNPLViewModel(repo, fetchStatusRepo, analyticsRepo)

        viewModel.onSearch("lazy")

        val names = (viewModel.uiState.value as UiState.Success<List<*>>).data
            .map { (it as com.crossplatform.sdk.domain.model.SelectedPaymentMethod).displayName }
        assertEquals(listOf("LazyPay"), names)
    }

    @Test
    fun `postBNPLRequest with an html action opens the webview`() = runTest {
        repo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        repo.initiatePaymentResult = ApiResponse.Success(
            PaymentMethodPostResponse(
                transactionId = "txn_1", transactionTimestampLocale = "12 Aug 2026",
                status = TransactionStatus(status = "RequiresAction", reason = "", reasonCode = ""),
                actions = listOf(PaymentActions(type = "html", htmlPageString = "<html>bnpl</html>")),
                paymentMethod = Method(type = "BuyNowPayLater", brand = "BuyNowPayLater"),
            ),
            responseCode = 200,
        )
        val viewModel = BNPLViewModel(repo, fetchStatusRepo, analyticsRepo)

        viewModel.postBNPLRequest(instrumentValue = "Simpl")

        assertEquals("<html>bnpl</html>", viewModel.htmlString.value)
        assertTrue(viewModel.showWebview.value)
    }

    @Test
    fun `postBNPLRequest turns off the loading animation on error`() = runTest {
        repo.getPaymentMethodsResult = ApiResponse.Success(emptyList(), responseCode = 200)
        repo.initiatePaymentResult = ApiResponse.Error(message = "provider unavailable")
        val viewModel = BNPLViewModel(repo, fetchStatusRepo, analyticsRepo)

        viewModel.postBNPLRequest(instrumentValue = "Simpl")

        assertEquals(false, viewModel.isBoxPayAnimationVisible.value)
    }
}
