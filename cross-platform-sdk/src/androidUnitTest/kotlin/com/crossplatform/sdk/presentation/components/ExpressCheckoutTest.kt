package com.crossplatform.sdk.presentation.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.crossplatform.sdk.domain.handler.ExpressCheckoutPaymentHandler
import com.crossplatform.sdk.domain.handler.ExpressCheckoutPaymentRequest
import com.crossplatform.sdk.domain.handler.ExpressCheckoutPaymentResult
import com.crossplatform.sdk.domain.handler.ApplePayExpressCheckoutConfig
import com.crossplatform.sdk.domain.handler.GooglePayExpressCheckoutConfig
import com.crossplatform.sdk.domain.handler.RevolutPayExpressCheckoutConfig
import com.crossplatform.sdk.presentation.theme.ProvideSDKFonts
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/** Fully controllable fake so tests can decide which express-checkout buttons "exist" on device. */
private class FakeExpressCheckoutPaymentHandler(
    private val googlePayAvailable: Boolean = false,
    private val applePayAvailable: Boolean = false,
    private val revolutPayAvailable: Boolean = false,
) : ExpressCheckoutPaymentHandler {
    override suspend fun isGooglePayAvailable(config: GooglePayExpressCheckoutConfig) = googlePayAvailable
    override fun isApplePayAvailable() = applePayAvailable
    override fun isRevolutPayAvailable() = revolutPayAvailable
    override fun launchGooglePay(request: ExpressCheckoutPaymentRequest, config: GooglePayExpressCheckoutConfig, onResult: (ExpressCheckoutPaymentResult) -> Unit) {}
    override fun launchApplePay(request: ExpressCheckoutPaymentRequest, config: ApplePayExpressCheckoutConfig, onResult: (ExpressCheckoutPaymentResult) -> Unit) {}
    override fun launchRevolutPay(request: ExpressCheckoutPaymentRequest, config: RevolutPayExpressCheckoutConfig, isSandbox: Boolean, onResult: (ExpressCheckoutPaymentResult) -> Unit) {}
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExpressCheckoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val config = GooglePayExpressCheckoutConfig(
        gateway = "example", merchantId = "merchant_1", merchantName = "Test Merchant",
        allowedPaymentMethods = emptyList(), siteReference = "site_1",
    )

    @Test
    fun `none of the buttons render when no express method is available`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                ExpressCheckout(
                    config = config,
                    paymentHandler = FakeExpressCheckoutPaymentHandler(),
                    onClickRevolut = {}, onClickGooglePay = {}, onClickApplePay = {},
                )
            }
        }

        composeTestRule.onAllNodesWithText("Pay").assertCountEquals(0)
    }

    @Test
    fun `only Google Pay shows when only it is available, and clicking it fires the callback`() {
        var googlePayClicked = false
        composeTestRule.setContent {
            ProvideSDKFonts {
                ExpressCheckout(
                    config = config,
                    paymentHandler = FakeExpressCheckoutPaymentHandler(googlePayAvailable = true),
                    onClickRevolut = {}, onClickGooglePay = { googlePayClicked = true }, onClickApplePay = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("google pay ").assertExists()
        composeTestRule.onNodeWithContentDescription("apple pay ").assertDoesNotExist()

        composeTestRule.onNodeWithContentDescription("google pay ").performClick()
        assertTrue(googlePayClicked)
    }

    @Test
    fun `all three buttons show when all are available`() {
        composeTestRule.setContent {
            ProvideSDKFonts {
                ExpressCheckout(
                    config = config,
                    paymentHandler = FakeExpressCheckoutPaymentHandler(
                        googlePayAvailable = true, applePayAvailable = true, revolutPayAvailable = true,
                    ),
                    onClickRevolut = {}, onClickGooglePay = {}, onClickApplePay = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("google pay ").assertExists()
        composeTestRule.onNodeWithContentDescription("apple pay ").assertExists()
        composeTestRule.onNodeWithContentDescription("revolut pay ").assertExists()
    }

    @Test
    fun `tapping Apple Pay fires its own callback, not Google Pay's`() {
        var applePayClicked = false
        var googlePayClicked = false
        composeTestRule.setContent {
            ProvideSDKFonts {
                ExpressCheckout(
                    config = config,
                    paymentHandler = FakeExpressCheckoutPaymentHandler(applePayAvailable = true),
                    onClickRevolut = {}, onClickGooglePay = { googlePayClicked = true }, onClickApplePay = { applePayClicked = true },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("apple pay ").performClick()

        assertTrue(applePayClicked)
        assertTrue(!googlePayClicked)
    }
}
