package com.crossplatform.sdk.presentation.viewmodel

import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.RecommendedInstrumentsResponse
import com.crossplatform.sdk.data.repo.MainScreenRepoImpl
import com.crossplatform.sdk.fakes.FakeApiService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** [MainScreenRepoImpl.editSavedCard] forwards to the API service and returns its response unchanged. */
@OptIn(ExperimentalCoroutinesApi::class)
class EditSavedCardRepoIntegrationTest {

    private val api = FakeApiService()
    private val repo = MainScreenRepoImpl(apiService = api, ioDispatcher = UnconfinedTestDispatcher())

    private fun instrument(nickName: String?) = RecommendedInstrumentsResponse(
        type = "Card", brand = "VISA", instrumentRef = "card_1",
        displayValue = "•••• 1111", logoUrl = null, cardNickName = nickName,
    )

    @Test
    fun `editSavedCard forwards the nickname and instrument ref to the API`() = runTest {
        api.nextEditSavedCard = ApiResponse.Success(instrument("Travel"), responseCode = 200)

        repo.editSavedCard(newNickName = "Travel", instrumentRef = "card_1")

        assertTrue(api.callLog.contains("editSavedCard(nickName=Travel, instrumentRef=card_1)"))
    }

    @Test
    fun `editSavedCard returns the API success response`() = runTest {
        api.nextEditSavedCard = ApiResponse.Success(instrument("Travel"), responseCode = 200)

        val result = repo.editSavedCard("Travel", "card_1")

        assertTrue(result is ApiResponse.Success)
        assertEquals("Travel", (result as ApiResponse.Success).data.cardNickName)
    }

    @Test
    fun `editSavedCard returns the API error response`() = runTest {
        api.nextEditSavedCard = ApiResponse.Error(message = "invalid nickname")

        val result = repo.editSavedCard("Travel", "card_1")

        assertTrue(result is ApiResponse.Error)
        assertEquals("invalid nickname", (result as ApiResponse.Error).message)
    }
}
