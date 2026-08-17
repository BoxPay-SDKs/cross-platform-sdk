package com.crossplatform.sdk.presentation.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.data.model.FetchSavedAddress
import com.crossplatform.sdk.fakes.FakeAddressScreenRepo
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SavedAddressScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val addressScreenRepo = FakeAddressScreenRepo()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        startKoin { modules(testKoinModule(addressScreenRepo = addressScreenRepo)) }
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    private fun address(labelType: String, city: String) = FetchSavedAddress(
        address1 = "123 Main St", address2 = null, city = city, state = "MH", countryCode = "IN",
        postalCode = "400001", shopperRef = "shopper_1", addressRef = "addr_1", labelType = labelType,
        labelName = null, name = "Jane Doe", email = "jane@example.com", phoneNumber = "9999999999",
    )

    @Test
    fun `on success, renders one SavedAddressCard per saved address`() {
        addressScreenRepo.savedAddressResult = ApiResponse.Success(
            listOf(address(labelType = "Home", city = "Mumbai"), address(labelType = "Work", city = "Pune")),
            responseCode = 200,
        )

        composeTestRule.setContent {
            ProvideSDKFonts { SavedAddressScreen(onBackPress = {}, buttonColor = "#000000") }
        }

        // address1/address2/city/state/pinCode render as one concatenated
        // AnnotatedString node (see SavedAddressCard.kt), so match by
        // substring rather than exact text.
        composeTestRule.onNodeWithText("Mumbai", substring = true).assertExists()
        composeTestRule.onNodeWithText("Pune", substring = true).assertExists()
    }

    @Test
    fun `on error, shows the error message`() {
        addressScreenRepo.savedAddressResult = ApiResponse.Error(message = "could not load addresses")

        composeTestRule.setContent {
            ProvideSDKFonts { SavedAddressScreen(onBackPress = {}, buttonColor = "#000000") }
        }

        composeTestRule.onNodeWithText("Welcome to error screen could not load addresses").assertExists()
    }
}
