package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.model.FetchSavedAddress
import com.crossplatform.sdk.data.repo.AddressScreenRepoImpl
import com.crossplatform.sdk.fakes.FakeApiService
import com.crossplatform.sdk.fakes.FakeCallUIAnalyticsRepo
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for the address flow.
 *
 * Uses REAL [AddressScreenRepoImpl] backed by [FakeApiService], wired into
 * REAL [AddressScreenViewModel]. Tests:
 *  - getSavedAddress routes through real repo
 *  - Success state with address list
 *  - Error state on api failure
 *  - deleteSavedAddress routes through real repo with correct addressRef
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddressRepoIntegrationTest {

    private lateinit var api: FakeApiService
    private lateinit var addressRepo: AddressScreenRepoImpl
    private lateinit var viewModel: AddressScreenViewModel

    private fun buildAddress(
        addressRef: String = "addr_001",
        labelType: String = "HOME"
    ) = FetchSavedAddress(
        address1    = "123 Main Street",
        address2    = "Apt 4B",
        city        = "Mumbai",
        state       = "Maharashtra",
        countryCode = "IN",
        postalCode  = "400001",
        shopperRef  = "shopper_test",
        addressRef  = addressRef,
        labelType   = labelType,
        labelName   = null,
        name        = "Test User",
        email       = "test@example.com",
        phoneNumber = "+919876543210"
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        CheckoutDetailsHandler.resetToDefault()

        api         = FakeApiService()
        addressRepo = AddressScreenRepoImpl(apiService = api, ioDispatcher = UnconfinedTestDispatcher())

        // Pre-stub so VM init (getSavedAddress) has a response
        api.nextSavedAddress = ApiResponse.Success(emptyList(), responseCode = 200)

        viewModel = AddressScreenViewModel(
            repo          = addressRepo,
            analyticsRepo = FakeCallUIAnalyticsRepo()
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        CheckoutDetailsHandler.resetToDefault()
    }

    // ── getSavedAddress ──────────────────────────────────────────────────────

    @Test
    fun `AddressScreenViewModel init calls getSavedAddress through real repo`() = runTest {
        assertTrue(api.callLog.any { it.contains("getSavedAddress") })
    }

    @Test
    fun `getSavedAddress Success puts AddressScreenViewModel in Success state`() = runTest {
        // Create a new VM with pre-stubbed address list
        val addressApi = FakeApiService()
        addressApi.nextSavedAddress = ApiResponse.Success(
            listOf(buildAddress("addr_001", "HOME"), buildAddress("addr_002", "WORK")),
            responseCode = 200
        )
        val vm = AddressScreenViewModel(
            repo          = AddressScreenRepoImpl(apiService = addressApi, ioDispatcher = UnconfinedTestDispatcher()),
            analyticsRepo = FakeCallUIAnalyticsRepo()
        )

        assertIs<UiState.Success<List<FetchSavedAddress>>>(vm.savedList.value)
        val list = (vm.savedList.value as UiState.Success).data
        assertEquals(2, list.size)
        assertEquals("addr_001", list[0].addressRef)
    }

    @Test
    fun `getSavedAddress Error puts AddressScreenViewModel in Error state`() = runTest {
        val errorApi = FakeApiService()
        errorApi.nextSavedAddress = ApiResponse.Error(message = "address load failed")
        val vm = AddressScreenViewModel(
            repo          = AddressScreenRepoImpl(apiService = errorApi, ioDispatcher = UnconfinedTestDispatcher()),
            analyticsRepo = FakeCallUIAnalyticsRepo()
        )

        assertIs<UiState.Error>(vm.savedList.value)
        assertEquals("address load failed", (vm.savedList.value as UiState.Error).message)
    }

    @Test
    fun `getSavedAddress routes through real AddressScreenRepoImpl to FakeApiService`() = runTest {
        api.nextSavedAddress = ApiResponse.Success(
            listOf(buildAddress("addr_home", "HOME")),
            responseCode = 200
        )

        val result = addressRepo.getSavedAddress()

        assertIs<ApiResponse.Success<List<FetchSavedAddress>>>(result)
        assertEquals(1, (result as ApiResponse.Success).data.size)
        assertEquals("addr_home", result.data[0].addressRef)
        assertTrue(api.callLog.any { it.contains("getSavedAddress") })
    }

    @Test
    fun `getSavedAddress returns empty list via real repo`() = runTest {
        api.nextSavedAddress = ApiResponse.Success(emptyList(), responseCode = 200)

        val result = addressRepo.getSavedAddress()

        assertIs<ApiResponse.Success<List<FetchSavedAddress>>>(result)
        assertEquals(0, (result as ApiResponse.Success).data.size)
    }

    // ── deleteSavedAddress ───────────────────────────────────────────────────

    @Test
    fun `deleteSavedAddress routes through real AddressScreenRepoImpl to FakeApiService`() = runTest {
        api.nextDeleteSavedAddress = ApiResponse.Success(
            buildAddress("addr_001"),
            responseCode = 200
        )

        viewModel.deleteSavedAddress(addressRef = "addr_001")

        assertNotNull(api.callLog.firstOrNull { it.contains("deleteSavedAddress") })
    }

    @Test
    fun `deleteSavedAddress forwards correct addressRef through real repo`() = runTest {
        api.nextDeleteSavedAddress = ApiResponse.Success(
            buildAddress("addr_work", "WORK"),
            responseCode = 200
        )

        val result = addressRepo.deleteSavedAddress(addressRef = "addr_work")

        assertIs<ApiResponse.Success<FetchSavedAddress>>(result)
        assertEquals("addr_work", (result as ApiResponse.Success).data.addressRef)
        assertTrue(api.callLog.any { it.contains("addr_work") })
    }

    @Test
    fun `deleteSavedAddress error returns ApiResponse Error via real repo`() = runTest {
        api.nextDeleteSavedAddress = ApiResponse.Error(message = "delete failed")

        val result = addressRepo.deleteSavedAddress(addressRef = "addr_nonexistent")

        assertIs<ApiResponse.Error<FetchSavedAddress>>(result)
        assertEquals("delete failed", (result as ApiResponse.Error).message)
    }

    // ── getSavedAddress can be refreshed ─────────────────────────────────────

    @Test
    fun `getSavedAddress can be called multiple times returning fresh data`() = runTest {
        // First call (during init) returns empty list
        assertEquals(0, (viewModel.savedList.value as? UiState.Success)?.data?.size ?: -1)

        // Re-stub and refresh
        api.nextSavedAddress = ApiResponse.Success(
            listOf(buildAddress("addr_new", "HOME")),
            responseCode = 200
        )
        viewModel.getSavedAddress()

        val list = (viewModel.savedList.value as? UiState.Success)?.data
        assertEquals(1, list?.size)
        assertEquals("addr_new", list?.firstOrNull()?.addressRef)
    }

    @Test
    fun `multiple addresses returned correctly via real repo`() = runTest {
        val addresses = (1..3).map { buildAddress("addr_00$it", "HOME") }
        api.nextSavedAddress = ApiResponse.Success(addresses, responseCode = 200)

        val result = addressRepo.getSavedAddress()

        assertIs<ApiResponse.Success<List<FetchSavedAddress>>>(result)
        assertEquals(3, (result as ApiResponse.Success).data.size)
    }
}
