package com.crossplatform.sdk.data.repo

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.FetchSavedAddress
import com.crossplatform.sdk.fakes.FakeApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AddressScreenRepoImplTest {

    private fun address(addressRef: String = "addr_1", city: String = "Mumbai") = FetchSavedAddress(
        address1 = "123 Main St", address2 = null, city = city, state = "MH", countryCode = "IN",
        postalCode = "400001", shopperRef = "shopper_1", addressRef = addressRef, labelType = "HOME",
        labelName = null, name = "Jane Doe", email = "jane@example.com", phoneNumber = "9999999999",
    )

    @Test
    fun `getSavedAddress returns the list from the api service unchanged`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextSavedAddress = ApiResponse.Success(listOf(address(city = "Mumbai"), address(city = "Delhi")), responseCode = 200)
        }
        val repo = AddressScreenRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

        val result = repo.getSavedAddress()

        assertIs<ApiResponse.Success<List<FetchSavedAddress>>>(result)
        assertEquals(listOf("Mumbai", "Delhi"), result.data.map { it.city })
    }

    @Test
    fun `getSavedAddress returns an empty list without treating it as an error`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextSavedAddress = ApiResponse.Success(emptyList(), responseCode = 200)
        }
        val repo = AddressScreenRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

        val result = repo.getSavedAddress()

        assertIs<ApiResponse.Success<List<FetchSavedAddress>>>(result)
        assertEquals(emptyList(), result.data)
    }

    @Test
    fun `deleteSavedAddress forwards the addressRef and returns the deleted entry`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextDeleteSavedAddress = ApiResponse.Success(address(addressRef = "addr_42"), responseCode = 200)
        }
        val repo = AddressScreenRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

        val result = repo.deleteSavedAddress(addressRef = "addr_42")

        assertIs<ApiResponse.Success<FetchSavedAddress>>(result)
        assertEquals("addr_42", result.data.addressRef)
        assertEquals("deleteSavedAddress(addr_42)", fakeApi.callLog.single())
    }

    @Test
    fun `deleteSavedAddress propagates an error`() = runTest {
        val fakeApi = FakeApiService().apply {
            nextDeleteSavedAddress = ApiResponse.Error(message = "address not found")
        }
        val repo = AddressScreenRepoImpl(apiService = fakeApi, ioDispatcher = Dispatchers.Unconfined)

        val result = repo.deleteSavedAddress(addressRef = "missing")

        assertIs<ApiResponse.Error>(result)
    }
}
