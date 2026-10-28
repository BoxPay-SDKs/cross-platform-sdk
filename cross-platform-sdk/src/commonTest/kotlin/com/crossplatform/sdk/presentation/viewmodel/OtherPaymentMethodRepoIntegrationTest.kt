package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.repo.FetchStatusRepoImpl
import com.crossplatform.sdk.data.repo.OtherPaymentMethodRepoImpl
import com.crossplatform.sdk.domain.model.UiState
import com.crossplatform.sdk.fakes.FakeApiService
import com.crossplatform.sdk.fakes.FakeCallUIAnalyticsRepo
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for Wallet, NetBanking, BNPL, and EMI payment flows.
 *
 * All tests use REAL [OtherPaymentMethodRepoImpl] + REAL [FetchStatusRepoImpl]
 * backed by [FakeApiService], wired into their respective REAL ViewModels.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OtherPaymentMethodRepoIntegrationTest {

    private lateinit var api: FakeApiService
    private lateinit var otherRepo: OtherPaymentMethodRepoImpl
    private lateinit var fetchStatusRepo: FetchStatusRepoImpl
    private lateinit var walletVm: WalletViewModel
    private lateinit var netBankingVm: NetBankingViewModel
    private lateinit var bnplVm: BNPLViewModel
    private lateinit var emiVm: EMIScreenViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        CheckoutDetailsHandler.resetToDefault()
        CheckoutDetailsHandler.setCheckoutToken(
            shopperToken            = null,
            token                   = "other_token",
            isTestEnv               = true,
            isSuccessScreenVisible  = true,
            ctaBorderRadius         = 8f,
            isSICheckboxChecked     = false,
            isSICheckboxEnabled     = false,
            isFailedScreenVisible   = true,
            showQROnLoad            = false,
            focusedTextInputBorderColor   = null,
            unfocusedTextInputBorderColor = null
        )

        api             = FakeApiService()
        otherRepo       = OtherPaymentMethodRepoImpl(apiService = api, ioDispatcher = UnconfinedTestDispatcher())
        fetchStatusRepo = FetchStatusRepoImpl(apiService = api, ioDispatcher = UnconfinedTestDispatcher())

        // Each VM's init calls loadList() which calls getPaymentMethods; pre-stub it
        api.nextPaymentMethods = ApiResponse.Success(
            listOf(buildPaymentMethod(id = "pm_wallet", type = "Wallet", brand = "GPAY")),
            responseCode = 200
        )

        walletVm    = WalletViewModel(repo = otherRepo, analyticsRepo = FakeCallUIAnalyticsRepo(), fetchStatusRepo = fetchStatusRepo)
        netBankingVm = NetBankingViewModel(repo = otherRepo, analyticsRepo = FakeCallUIAnalyticsRepo(), fetchStatusRepo = fetchStatusRepo)
        bnplVm      = BNPLViewModel(repo = otherRepo, analyticsRepo = FakeCallUIAnalyticsRepo(), fetchStatusRepo = fetchStatusRepo)
        emiVm       = EMIScreenViewModel(repo = otherRepo, analyticsRepo = FakeCallUIAnalyticsRepo(), fetchStatusRepo = fetchStatusRepo)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        CheckoutDetailsHandler.resetToDefault()
    }

    // ── getPaymentMethods via real OtherPaymentMethodRepoImpl ────────────────

    @Test
    fun `WalletViewModel init loads payment methods through real OtherPaymentMethodRepoImpl`() = runTest {
        // The WalletViewModel init triggered loadWalletList() which called the real repo
        assertTrue(api.callLog.any { it.contains("fetchPaymentMethods") })
    }

    @Test
    fun `WalletViewModel shows Success after real repo returns payment methods`() = runTest {
        assertIs<UiState.Success<*>>(walletVm.uiState.value)
    }

    @Test
    fun `getPaymentMethods error puts WalletViewModel in Error state`() = runTest {
        // Create a new VM with an error-configured api
        val errorApi = FakeApiService()
        errorApi.nextPaymentMethods = ApiResponse.Error(message = "timeout")
        val errorVm = WalletViewModel(
            repo            = OtherPaymentMethodRepoImpl(apiService = errorApi, ioDispatcher = UnconfinedTestDispatcher()),
            analyticsRepo   = FakeCallUIAnalyticsRepo(),
            fetchStatusRepo = FetchStatusRepoImpl(apiService = errorApi, ioDispatcher = UnconfinedTestDispatcher())
        )
        assertIs<UiState.Error>(errorVm.uiState.value)
    }

    // ── Wallet payment ───────────────────────────────────────────────────────

    @Test
    fun `wallet postWalletRequest via real OtherPaymentMethodRepoImpl routes methodsPostRequest`() = runTest {
        api.nextMethodsPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "redirect", url = "https://gpay.example.com"),
            responseCode = 200
        )

        walletVm.postWalletRequest(instrumentValue = "GPAY", surcharge = null)

        assertTrue(walletVm.showWebview.value)
        assertTrue(api.callLog.any { it.contains("methodsPostRequest") })
    }

    @Test
    fun `wallet postWalletRequest surcharges forwarded through real repo`() = runTest {
        api.nextMethodsPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "redirect", url = "https://wallet.example.com"),
            responseCode = 200
        )

        walletVm.postWalletRequest(instrumentValue = "PHONEPE", surcharge = listOf("SC_WALLET"))

        assertEquals(listOf("SC_WALLET"), api.lastMethodsPostSurcharges)
    }

    @Test
    fun `wallet payment html action opens webview with correct html`() = runTest {
        api.nextMethodsPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "html", htmlContent = "<html>wallet3ds</html>"),
            responseCode = 200
        )

        walletVm.postWalletRequest(instrumentValue = "MOBIKWIK", surcharge = null)

        assertTrue(walletVm.showWebview.value)
        assertEquals("<html>wallet3ds</html>", walletVm.htmlString.value)
    }

    @Test
    fun `wallet payment api error hides loading indicator`() = runTest {
        api.nextMethodsPostResponse = ApiResponse.Error(message = "wallet timeout")

        walletVm.postWalletRequest(instrumentValue = "GPAY", surcharge = null)

        assertFalse(walletVm.isBoxPayAnimationVisible.value)
        assertFalse(walletVm.showWebview.value)
    }

    // ── NetBanking payment ───────────────────────────────────────────────────

    @Test
    fun `netBanking postNetBankingRequest via real repo routes methodsPostRequest`() = runTest {
        api.nextMethodsPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "redirect", url = "https://hdfc.bank.example.com"),
            responseCode = 200
        )

        netBankingVm.postNetBankingRequest(instrumentValue = "HDFC", surchargeList = null)

        assertTrue(netBankingVm.showWebview.value)
        assertEquals("https://hdfc.bank.example.com", netBankingVm.url.value)
    }

    @Test
    fun `netBanking surcharges forwarded through real OtherPaymentMethodRepoImpl`() = runTest {
        api.nextMethodsPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "redirect", url = "https://bank.example.com"),
            responseCode = 200
        )

        netBankingVm.postNetBankingRequest(instrumentValue = "ICICI", surchargeList = listOf("SC_NB"))

        assertEquals(listOf("SC_NB"), api.lastMethodsPostSurcharges)
    }

    // ── BNPL payment ─────────────────────────────────────────────────────────

    @Test
    fun `BNPL postBNPLRequest via real repo routes methodsPostRequest`() = runTest {
        api.nextMethodsPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "redirect", url = "https://bnpl.example.com"),
            responseCode = 200
        )

        bnplVm.postBNPLRequest(instrumentValue = "LAZYPAY", surcharge = null)

        assertTrue(bnplVm.showWebview.value)
        assertTrue(api.callLog.any { it.contains("methodsPostRequest") && it.contains("LAZYPAY") })
    }

    @Test
    fun `BNPL surcharges forwarded through real OtherPaymentMethodRepoImpl`() = runTest {
        api.nextMethodsPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "redirect", url = "https://bnpl.example.com"),
            responseCode = 200
        )

        bnplVm.postBNPLRequest(instrumentValue = "SIMPL", surcharge = listOf("SC_BNPL"))

        assertEquals(listOf("SC_BNPL"), api.lastMethodsPostSurcharges)
    }

    // ── EMI payment ──────────────────────────────────────────────────────────

    @Test
    fun `EMI initiateEMIPayment via real OtherPaymentMethodRepoImpl routes emiPostRequest`() = runTest {
        api.nextEmiPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "html", htmlContent = "<html>emi3ds</html>"),
            responseCode = 200
        )
        // EMIScreenViewModel reads from its own state fields
        emiVm.cardNumberText.value     = "4111111111111111"
        emiVm.cardHolderNameText.value = "EMI Tester"
        emiVm.cardExpiryText.value     = "12/28"
        emiVm.cardCvvText.value        = "123"

        emiVm.postEMIRequest(surcharge = null)

        assertTrue(emiVm.showWebview.value)
        assertTrue(api.callLog.any { it.contains("emiPostRequest") })
    }

    @Test
    fun `EMI surcharges forwarded through real OtherPaymentMethodRepoImpl`() = runTest {
        api.nextEmiPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "html"),
            responseCode = 200
        )
        emiVm.cardNumberText.value     = "4111111111111111"
        emiVm.cardHolderNameText.value = "EMI User"
        emiVm.cardExpiryText.value     = "12/28"
        emiVm.cardCvvText.value        = "123"

        emiVm.postEMIRequest(surcharge = listOf("SC_EMI"))

        assertEquals(listOf("SC_EMI"), api.lastEmiPostSurcharges)
    }

    @Test
    fun `EMI payment html action opens webview`() = runTest {
        api.nextEmiPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "html", htmlContent = "<html>emi_3ds</html>"),
            responseCode = 200
        )
        emiVm.cardNumberText.value     = "4111111111111111"
        emiVm.cardHolderNameText.value = "Duration Tester"
        emiVm.cardExpiryText.value     = "06/27"
        emiVm.cardCvvText.value        = "456"

        emiVm.postEMIRequest(surcharge = null)

        assertTrue(emiVm.showWebview.value)
        assertEquals("<html>emi_3ds</html>", emiVm.htmlString.value)
    }

    // ── fetchStatus after payment via real FetchStatusRepoImpl ───────────────

    @Test
    fun `wallet e2e - redirect then SUCCESS fetch status marks payment complete`() = runTest {
        // Step 1: Wallet payment → redirect
        api.nextMethodsPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "redirect", url = "https://gpay.example.com"),
            responseCode = 200
        )
        walletVm.postWalletRequest(instrumentValue = "GPAY", surcharge = null)
        assertTrue(walletVm.showWebview.value)

        // Step 2: fetchStatus → SUCCESS
        api.nextFetchStatus = ApiResponse.Success(
            buildSuccessFetchStatus(transactionId = "txn_wallet_001"),
            responseCode = 200
        )
        walletVm.callFetchStatus("SUCCESS")

        assertTrue(CheckoutDetailsHandler.isPaymentSuccessful)
        assertNotNull(api.callLog.firstOrNull { it.contains("methodsPostRequest") })
        assertNotNull(api.callLog.firstOrNull { it.contains("fetchStatus") })
    }

    @Test
    fun `netBanking e2e - redirect then EXPIRED fetch status marks payment expired`() = runTest {
        api.nextMethodsPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "redirect", url = "https://sbi.bank.example.com"),
            responseCode = 200
        )
        netBankingVm.postNetBankingRequest(instrumentValue = "SBI", surchargeList = null)

        api.nextFetchStatus = ApiResponse.Success(
            buildExpiredFetchStatus(),
            responseCode = 200
        )
        netBankingVm.callFetchStatus("EXPIRED")

        // Expired status routes to the failed/expired state via handlePaymentResponse
        // CheckoutDetailsHandler should not show success
        assertFalse(CheckoutDetailsHandler.isPaymentSuccessful)
    }

    @Test
    fun `BNPL e2e - redirect then FAILED fetch status marks payment failed`() = runTest {
        api.nextMethodsPostResponse = ApiResponse.Success(
            buildRequiresActionResponse(actionType = "redirect", url = "https://bnpl.example.com"),
            responseCode = 200
        )
        bnplVm.postBNPLRequest(instrumentValue = "LAZYPAY", surcharge = null)

        api.nextFetchStatus = ApiResponse.Success(
            buildFailedFetchStatus(reason = "BNPL declined"),
            responseCode = 200
        )
        bnplVm.callFetchStatus("FAILED")

        assertTrue(CheckoutDetailsHandler.isPaymentFailed)
    }
}
