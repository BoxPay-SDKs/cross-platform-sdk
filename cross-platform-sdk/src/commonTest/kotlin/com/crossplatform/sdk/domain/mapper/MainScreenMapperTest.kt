package com.crossplatform.sdk.domain.mapper

import com.crossplatform.sdk.data.handler.CheckoutDetailsHandler
import com.crossplatform.sdk.data.handler.UserDataHandler
import com.crossplatform.sdk.data.model.CheckoutTheme
import com.crossplatform.sdk.data.model.Configs
import com.crossplatform.sdk.data.model.DeliveryAddress
import com.crossplatform.sdk.data.model.EnabledFields
import com.crossplatform.sdk.data.model.MerchantDetails
import com.crossplatform.sdk.data.model.Money
import com.crossplatform.sdk.data.model.PaymentContext
import com.crossplatform.sdk.data.model.PaymentDetails
import com.crossplatform.sdk.data.model.SessionDetails
import com.crossplatform.sdk.data.model.Shopper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [formatDate] / [formatWords] are pure string-formatting helpers used by
 * [MainScreenMapper] to render subscription details on screen — tested in
 * isolation first since they're the easiest place a locale/format regression
 * would show up.
 *
 * The `toUiModel integration` group below is a broader test: it runs the
 * real mapper against a realistic [SessionDetails] payload and asserts on
 * the *shared handler state* ([CheckoutDetailsHandler], [UserDataHandler])
 * that the actual Compose screens read via `collectAsStateWithLifecycle()`.
 * This is closer to an integration test than a unit test — it exercises the
 * same data path the UI does, just without rendering it.
 */
class MainScreenMapperTest {

    // ── formatDate ──────────────────────────────────────────────────────────

    @Test
    fun `formatDate converts numeric ddMMyyyy to dd-MMM-yyyy`() {
        assertEquals("15-Jun-2026", formatDate("15/06/2026"))
    }

    @Test
    fun `formatDate handles single-digit day and month`() {
        assertEquals("05-Jan-2026", formatDate("05/01/2026"))
    }

    @Test
    fun `formatDate returns empty string for malformed input`() {
        assertEquals("", formatDate("not-a-date"))
        assertEquals("", formatDate("15/06"))
        assertEquals("", formatDate(""))
    }

    @Test
    fun `formatDate returns empty string for out-of-range month`() {
        assertEquals("", formatDate("15/13/2026"))
        assertEquals("", formatDate("15/00/2026"))
    }

    // ── formatWords ─────────────────────────────────────────────────────────

    @Test
    fun `formatWords inserts a space between lower-to-upper case transitions`() {
        assertEquals("Monthly Cycle", formatWords("MonthlyCycle"))
        assertEquals("Every Two Weeks", formatWords("EveryTwoWeeks"))
    }

    @Test
    fun `formatWords leaves single words untouched`() {
        assertEquals("Monthly", formatWords("Monthly"))
    }

    // ── toUiModel (integration) ────────────────────────────────────────────

    @BeforeTest
    fun setUp() {
        // CheckoutDetailsHandler / UserDataHandler each own a
        // CoroutineScope(Dispatchers.Main.immediate); a Main dispatcher must
        // be installed before the very first reference to those objects on
        // a plain JVM/native test runner.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toUiModel populates CheckoutDetailsHandler with theme and merchant details the UI reads`() {
        val session = sessionDetails(
            amount = 1999.0,
            currencySymbol = "₹",
            currencyCode = "INR",
            merchantName = "Acme Store",
            buttonColor = "#FF5722",
            buttonTextColor = "#FFFFFF",
            ctaTextFontSize = "18",
        )

        session.toUiModel()

        val details = CheckoutDetailsHandler.checkoutDetails
        assertEquals("Acme Store", details.merchantName)
        assertEquals("#FF5722", details.buttonColor)
        assertEquals("#FFFFFF", details.buttonTextColor)
        assertEquals("₹", details.currencySymbol)
        assertEquals("INR", details.currencyCode)
        assertEquals(18, details.ctaTextFontSize) // this is exactly what PayButton renders text at
    }

    @Test
    fun `toUiModel populates UserDataHandler from the shopper payload`() {
        val session = sessionDetails(shopperFirstName = "Jane", shopperLastName = "Doe", shopperEmail = "jane@example.com")

        session.toUiModel()

        val user = UserDataHandler.userData
        assertEquals("Jane", user.firstName)
        assertEquals("Doe", user.lastName)
        assertEquals("jane@example.com", user.email)
    }

    @Test
    fun `toUiModel maps enabled and editable fields from configs`() {
        val session = sessionDetails(
            enabledFields = listOf(
                EnabledFields(field = "SHOPPER_EMAIL", editable = true, mandatory = true),
                EnabledFields(field = "SHOPPER_NAME", editable = false, mandatory = false),
            )
        )

        session.toUiModel()

        val details = CheckoutDetailsHandler.checkoutDetails
        assertEquals(true, details.isEmailEnabled)
        assertEquals(true, details.isEmailEditable)
        assertEquals(true, details.isFullNameEnabled)
        assertEquals(false, details.isFullNameEditable)
        assertEquals(false, details.isPhoneEnabled) // absent from enabledFields
    }

    @Test
    fun `toUiModel result carries total amount and currency onto the UI model`() {
        val session = sessionDetails(amount = 499.50, currencySymbol = "$", currencyCode = "USD")

        val uiModel = session.toUiModel()

        assertEquals(499.50, uiModel.totalAmount)
        assertEquals("$", uiModel.currencySymbol)
        assertEquals("USD", uiModel.currencyCode)
    }

    @Test
    fun `toUiModel with no order returns null orderDetails`() {
        val session = sessionDetails(order = null)

        val uiModel = session.toUiModel()

        assertNull(uiModel.orderDetails)
    }

    // ── Fixture builder ─────────────────────────────────────────────────────

    private fun sessionDetails(
        amount: Double = 100.0,
        currencySymbol: String = "₹",
        currencyCode: String = "INR",
        merchantName: String = "Test Merchant",
        buttonColor: String = "#000000",
        buttonTextColor: String = "#FFFFFF",
        ctaTextFontSize: String = "16",
        shopperFirstName: String? = null,
        shopperLastName: String? = null,
        shopperEmail: String? = null,
        enabledFields: List<EnabledFields> = emptyList(),
        order: com.crossplatform.sdk.data.model.OrderDetails? = null,
    ) = SessionDetails(
        configs = Configs(paymentMethods = emptyList(), enabledFields = enabledFields),
        paymentDetails = PaymentDetails(
            context = PaymentContext(countryCode = "IN", localeCode = "en-IN"),
            money = Money(currencySymbol = currencySymbol, currencyCode = currencyCode, amount = amount),
            shopper = Shopper(
                firstName = shopperFirstName,
                lastName = shopperLastName,
                email = shopperEmail,
                uniqueReference = "shopper_1",
                deliveryAddress = DeliveryAddress(),
            ),
            subscriptionDetails = null,
            order = order,
        ),
        merchantDetails = MerchantDetails(
            merchantName = merchantName,
            merchantLogo = null,
            // NOTE: the mapper reads the CTA font size from
            // `checkoutTheme.payButtonFontSize` (there's no separate field for it).
            checkoutTheme = CheckoutTheme(
                primaryButtonColor = buttonColor,
                buttonTextColor = buttonTextColor,
                headerColor = "#000000",
                headerTextColor = "#FFFFFF",
                focusedTextInputBorderColor = "#CCCCCC",
                unfocusedTextInputBorderColor = "#DDDDDD",
                payButtonFontSize = ctaTextFontSize,
                font = "Inter",
                payButtonBorderRadius = "8",
            ),
            customFields = emptyList(),
        ),
        sessionExpiryTimestamp = "",
        status = "NOACTION",
        lastPaidAtTimestamp = null,
        lastTransactionId = null,
        lastTransactionDetails = null,
    )
}
