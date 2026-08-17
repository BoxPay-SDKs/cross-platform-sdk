package com.crossplatform.sdk.domain.mapper

import com.crossplatform.sdk.data.model.InstantOfferResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InstantOfferMapperTest {

    @BeforeTest
    fun setUp() {
        // toUiModel() reads CheckoutDetailsHandler.currencyFlow, which needs
        // a Main dispatcher installed before the singleton is first touched.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun offer(
        code: String = "SAVE10",
        title: String? = "Save 10%",
        description: String? = null,
        terms: String? = "T&C apply",
        discountType: String? = "PERCENTAGE",
        discountAmount: Double? = null,
        discountPercentage: Double? = 10.0,
        applicableBrand: String? = "VISA",
    ) = InstantOfferResponse(
        title = title,
        description = description,
        terms = terms,
        code = code,
        discount = InstantOfferResponse.Discount(amount = discountAmount, percentage = discountPercentage, type = discountType),
        enabled = true,
        criteria = InstantOfferResponse.InstantOfferCriteria(
            applicableTo = InstantOfferResponse.OfferApplicableTo(
                paymentMethods = listOf(InstantOfferResponse.OfferPaymentMethod(type = "Card", brand = applicableBrand))
            ),
            startDate = null,
            endDate = null,
        ),
    )

    @Test
    fun `description is preferred over title when both are present`() {
        val result = listOf(offer(description = "Flat discount", title = "Save 10%")).toUiModel()

        assertEquals("Flat discount", result.single().description)
    }

    @Test
    fun `falls back to title when description is null`() {
        val result = listOf(offer(description = null, title = "Save 10%")).toUiModel()

        assertEquals("Save 10%", result.single().description)
    }

    @Test
    fun `falls back to an empty string when both description and title are null`() {
        val result = listOf(offer(description = null, title = null)).toUiModel()

        assertEquals("", result.single().description)
    }

    @Test
    fun `maps discount type amount and percentage straight through`() {
        val result = listOf(offer(discountType = "FLAT", discountAmount = 50.0, discountPercentage = null)).toUiModel()

        val item = result.single()
        assertEquals("FLAT", item.discountType)
        assertEquals(50.0, item.discountAmount)
    }

    @Test
    fun `applicableOn reads the brand of the first applicable payment method`() {
        val result = listOf(offer(applicableBrand = "Mastercard")).toUiModel()

        assertEquals("Mastercard", result.single().applicableOn)
    }

    @Test
    fun `expiryDate is always null - not sourced from the response`() {
        val result = listOf(offer()).toUiModel()

        assertNull(result.single().expiryDate)
    }

    @Test
    fun `maps a list of offers preserving order and code`() {
        val result = listOf(offer(code = "A"), offer(code = "B")).toUiModel()

        assertEquals(listOf("A", "B"), result.map { it.code })
    }
}
