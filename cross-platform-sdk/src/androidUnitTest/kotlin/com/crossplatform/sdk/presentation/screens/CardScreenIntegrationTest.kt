package com.crossplatform.sdk.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.crossplatform.sdk.data.ApiResponse
import com.crossplatform.sdk.fakes.FakeCallUIAnalyticsRepo
import com.crossplatform.sdk.fakes.FakeCardScreenRepo
import com.crossplatform.sdk.fakes.FakeFetchStatusRepo
import com.crossplatform.sdk.presentation.components.CardComponent
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import com.crossplatform.sdk.presentation.viewmodel.CardScreenViewModel
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * True end-to-end integration test for the card entry screen: a real
 * [CardScreenViewModel] (backed only by fakes at the network boundary) wired
 * to the real [CardComponent] exactly the way [CardScreen] wires it in
 * production. This exercises the full path a user actually triggers:
 *
 *   Compose text input -> viewModel.handleCardNumberChange() -> Luhn/format
 *   validation -> viewModel.cardNumberError state -> recomposition ->
 *   ErrorText rendered (or not) on screen.
 *
 * Unlike [com.crossplatform.sdk.presentation.components.PayButtonTest],
 * which asserts on a composable driven by static props, this test proves the
 * ViewModel's real validation logic actually reaches the screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CardScreenIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: CardScreenViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = CardScreenViewModel(
            repo = FakeCardScreenRepo().apply {
                // BIN lookup (fires once 9 digits are typed) — keep it a
                // harmless no-op error so the test isn't tripped up by it.
                getCardDetailsResult = ApiResponse.Error(message = "not stubbed for this test")
            },
            fetchStatusRepo = FakeFetchStatusRepo(),
            analyticsRepo = FakeCallUIAnalyticsRepo(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `typing a card number starting with zero shows the real validation error on screen`() {
        composeTestRule.setContent {
            ProvideSDKFonts { TestCardScreenHost(viewModel) }
        }

        composeTestRule.onNodeWithText("Card Number*").performTextInput("0111111111111111")

        // The default error text CardScreenViewModel seeds every field with.
        composeTestRule.onNodeWithText("Required").assertExists()
        // Same assertion against the ViewModel directly — confirms the UI
        // assertion above is actually backed by real state, not a stale node.
        assertTrue(viewModel.cardNumberError.value)
    }

    @Test
    fun `typing a well-formed card number does not show a validation error`() {
        composeTestRule.setContent {
            ProvideSDKFonts { TestCardScreenHost(viewModel) }
        }

        // Luhn-valid Visa test number, not starting with 0.
        composeTestRule.onNodeWithText("Card Number*").performTextInput("4532015112830366")

        composeTestRule.onNodeWithText("Required").assertDoesNotExist()
        assertFalse(viewModel.cardNumberError.value)
    }
}

/**
 * Minimal stand-in for [CardScreen]'s wiring — same field-to-callback
 * mapping, with the parts that depend on Koin/navigation (which aren't
 * relevant to the validation flow under test) replaced by fixed values.
 */
@Composable
private fun TestCardScreenHost(viewModel: CardScreenViewModel) {
    var isSICheckboxChecked by remember { mutableStateOf(false) }

    CardComponent(
        isSICheckboxChecked = isSICheckboxChecked,
        isSICheckboxEnabled = false,
        isSubscriptionCheckout = false,
        isSubscriptionDetailsVisible = false,
        onClickCheckBoxItem = { isSICheckboxChecked = it },
        onClickShowKnowMoreDialog = { viewModel.showKnowMoreDialog.value = true },
        onClickCVVInfo = { viewModel.showCvvInfo.value = true },
        onClickSavedCardCheckBox = {
            viewModel.isSavedCardCheckBoxClicked.value = !viewModel.isSavedCardCheckBoxClicked.value
        },
        shopperToken = null,
        subscription = null,
        currencySymbol = "INR",
        cardNumberText = viewModel.cardNumberText.value,
        cardHolderNameText = viewModel.cardHolderNameText.value,
        cardExpiryText = viewModel.cardExpiryText.value,
        cardCvvText = viewModel.cardCvvText.value,
        cardNickNameText = viewModel.cardNickNameText.value,
        cardNumberError = viewModel.cardNumberError.value,
        cardHolderNameError = viewModel.cardHolderNameError.value,
        cardExpiryError = viewModel.cardExpiryError.value,
        cardCvvError = viewModel.cardCvvError.value,
        maxCardNumberLength = viewModel.maxCardNumberLength.value,
        maxCvvLength = viewModel.maxCvvLength.value,
        handleCardNumberChange = { viewModel.handleCardNumberChange(it, isTestEnv = false) },
        handleCardHolderNameChange = {
            viewModel.cardHolderNameText.value = it
            if (it.isNotBlank()) viewModel.cardHolderNameError.value = false
            viewModel.checkCardValid(isTestEnv = false)
        },
        handleExpiryChange = { viewModel.handleExpiryChange(it, isTestEnv = false) },
        handleCvvChange = {
            viewModel.cardCvvText.value = it
            viewModel.checkCardValid(isTestEnv = false)
        },
        cardSelectedIcon = viewModel.cardSelectedIcon.value,
        setCardNumberError = { viewModel.cardNumberError.value = false },
        setCardHolderNameError = { viewModel.cardHolderNameError.value = false },
        setCardExpiryError = { viewModel.cardExpiryError.value = false },
        setCardCvvError = { viewModel.cardCvvError.value = false },
        unfocusedTextInputBorderColor = "#DDDDDD",
        focusedTextInputBorderColor = "#CCCCCC",
        buttonColor = "#000000",
        onBlurCardNumber = {},
        onBlurCardName = {},
        onBlurCardExpiry = {},
        onBlurCardCVV = {},
        cardNumberErrorText = viewModel.cardNumberErrorText.value,
        cardHolderNameErrorText = viewModel.cardHolderNameErrorText.value,
        cardExpiryErrorText = viewModel.cardExpiryErrorText.value,
        cardCvvErrorText = viewModel.cardCvvErrorText.value,
        amount = 499.0,
        cardValid = viewModel.cardValid.value,
        isBoxPayPayButtonVisible = true,
        postCardRequest = { viewModel.postCardRequest(isSICheckBoxClicked = it) },
        buttonTextColor = "#FFFFFF",
        ctaBorderRadius = 8,
        isSavedCardCheckBoxClicked = viewModel.isSavedCardCheckBoxClicked.value,
        modifier = Modifier,
    )
}
