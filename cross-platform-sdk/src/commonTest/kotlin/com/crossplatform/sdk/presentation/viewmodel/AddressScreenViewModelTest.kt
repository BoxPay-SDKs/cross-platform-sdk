package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.FetchSavedAddress
import com.crossplatform.sdk.fakes.FakeAddressScreenRepo
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

@OptIn(ExperimentalCoroutinesApi::class)
class AddressScreenViewModelTest {

    private lateinit var repo: FakeAddressScreenRepo
    private lateinit var analyticsRepo: FakeCallUIAnalyticsRepo

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = FakeAddressScreenRepo()
        analyticsRepo = FakeCallUIAnalyticsRepo()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun address(addressRef: String, city: String) = FetchSavedAddress(
        address1 = "123 Main St", address2 = null, city = city, state = "MH", countryCode = "IN",
        postalCode = "400001", shopperRef = "shopper_1", addressRef = addressRef, labelType = "Home",
        labelName = null, name = "Jane Doe", email = "jane@example.com", phoneNumber = "9999999999",
    )

    @Test
    fun `on init, the saved address list loads into savedList`() = runTest {
        repo.savedAddressResult = ApiResponse.Success(listOf(address("addr_1", "Mumbai"), address("addr_2", "Pune")), responseCode = 200)

        val viewModel = AddressScreenViewModel(repo, analyticsRepo)

        assertIs<UiState.Success<List<FetchSavedAddress>>>(viewModel.savedList.value)
        val cities = (viewModel.savedList.value as UiState.Success<List<FetchSavedAddress>>).data.map { it.city }
        assertEquals(listOf("Mumbai", "Pune"), cities)
    }

    @Test
    fun `on init, an error is surfaced as UiState_Error`() = runTest {
        repo.savedAddressResult = ApiResponse.Error(message = "could not load addresses")

        val viewModel = AddressScreenViewModel(repo, analyticsRepo)

        assertIs<UiState.Error>(viewModel.savedList.value)
        assertEquals("could not load addresses", (viewModel.savedList.value as UiState.Error).message)
    }

    @Test
    fun `getSavedAddress can be re-triggered manually and refreshes the list`() = runTest {
        repo.savedAddressResult = ApiResponse.Success(listOf(address("addr_1", "Mumbai")), responseCode = 200)
        val viewModel = AddressScreenViewModel(repo, analyticsRepo)

        repo.savedAddressResult = ApiResponse.Success(listOf(address("addr_2", "Delhi")), responseCode = 200)
        viewModel.getSavedAddress()

        val cities = (viewModel.savedList.value as UiState.Success<List<FetchSavedAddress>>).data.map { it.city }
        assertEquals(listOf("Delhi"), cities)
    }

    @Test
    fun `deleteSavedAddress forwards the address ref to the repo`() = runTest {
        repo.savedAddressResult = ApiResponse.Success(emptyList(), responseCode = 200)
        repo.deleteSavedAddressResult = ApiResponse.Success(address("addr_1", "Mumbai"), responseCode = 200)
        val viewModel = AddressScreenViewModel(repo, analyticsRepo)

        viewModel.deleteSavedAddress(addressRef = "addr_1")

        assertEquals("addr_1", repo.lastDeletedAddressRef)
    }

    @Test
    fun `selectedSavedAddress starts empty`() = runTest {
        repo.savedAddressResult = ApiResponse.Success(emptyList(), responseCode = 200)

        val viewModel = AddressScreenViewModel(repo, analyticsRepo)

        assertEquals("", viewModel.selectedSavedAddress.value)
    }
}
