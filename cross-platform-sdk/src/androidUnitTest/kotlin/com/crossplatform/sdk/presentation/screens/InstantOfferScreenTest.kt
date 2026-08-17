package com.crossplatform.sdk.presentation.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.InstantOfferResponse
import com.crossplatform.sdk.fakes.FakeInstantOfferRepo
import com.crossplatform.sdk.fakes.testKoinModule
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InstantOfferScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val instantOfferRepo = FakeInstantOfferRepo()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        startKoin { modules(testKoinModule(instantOfferRepo = instantOfferRepo)) }
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun offer(code: String) = InstantOfferResponse(
        title = "Save on $code", description = null, terms = "T&C apply", code = code,
        discount = InstantOfferResponse.Discount(amount = null, percentage = 10.0, type = "Percentage"),
        enabled = true,
        criteria = InstantOfferResponse.InstantOfferCriteria(
            applicableTo = InstantOfferResponse.OfferApplicableTo(paymentMethods = emptyList()),
            startDate = null, endDate = null,
        ),
    )

    @Test
    fun `on success, renders one OfferCard per offer`() {
        instantOfferRepo.offersResult = ApiResponse.Success(listOf(offer("SAVE10"), offer("WELCOME50")), responseCode = 200)

        composeTestRule.setContent {
            ProvideSDKFonts { InstantOfferScreen(selectedCode = "", onBackPress = {}, onClickApply = {}, onClickRemove = {}) }
        }

        composeTestRule.onNodeWithText("SAVE10").assertExists()
        composeTestRule.onNodeWithText("WELCOME50").assertExists()
    }

    @Test
    fun `on error, shows the error message`() {
        instantOfferRepo.offersResult = ApiResponse.Error(message = "could not load offers")

        composeTestRule.setContent {
            ProvideSDKFonts { InstantOfferScreen(selectedCode = "", onBackPress = {}, onClickApply = {}, onClickRemove = {}) }
        }

        composeTestRule.onNodeWithText("Welcome to error screen could not load offers").assertExists()
    }

    @Test
    fun `tapping APPLY on an offer invokes onClickApply with that offer's code`() {
        instantOfferRepo.offersResult = ApiResponse.Success(listOf(offer("SAVE10")), responseCode = 200)
        var appliedCode: String? = null

        composeTestRule.setContent {
            ProvideSDKFonts {
                InstantOfferScreen(selectedCode = "", onBackPress = {}, onClickApply = { appliedCode = it }, onClickRemove = {})
            }
        }
        composeTestRule.onNodeWithText("APPLY").performClick()

        assertEquals("SAVE10", appliedCode)
    }

    @Test
    fun `when selectedCode matches an offer, that card shows REMOVE and tapping it invokes onClickRemove`() {
        instantOfferRepo.offersResult = ApiResponse.Success(listOf(offer("SAVE10")), responseCode = 200)
        var removeClicked = false

        composeTestRule.setContent {
            ProvideSDKFonts {
                InstantOfferScreen(selectedCode = "SAVE10", onBackPress = {}, onClickApply = {}, onClickRemove = { removeClicked = true })
            }
        }
        composeTestRule.onNodeWithText("REMOVE").performClick()

        assertEquals(true, removeClicked)
    }
}
