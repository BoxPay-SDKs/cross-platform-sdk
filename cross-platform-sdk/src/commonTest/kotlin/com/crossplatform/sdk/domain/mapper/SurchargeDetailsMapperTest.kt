package com.crossplatform.sdk.domain.mapper

import com.crossplatform.sdk.data.model.FetchSurchargeResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class SurchargeDetailsMapperTest {

    private fun surchargeDetails(
        title: String? = "Card Surcharge",
        surchargeCode: String? = "SC1",
        applicableOn: String? = "credit",
        network: String? = "VISA",
        classification: String? = "CONSUMER",
    ) = FetchSurchargeResponse.SurchargeDetails(
        title = title,
        surchargeCode = surchargeCode,
        applicableOn = applicableOn,
        network = network,
        classification = classification,
    )

    private fun response(
        appliedCharges: List<FetchSurchargeResponse.AppliedSurcharge>?
    ) = FetchSurchargeResponse(
        amountBeforeSurcharge = null,
        appliedCharges = appliedCharges,
        finalAmountAfterMarriage = null,
    )

    @Test
    fun `null appliedCharges maps to an empty list rather than throwing`() {
        val result = response(appliedCharges = null).toUiModel()

        assertEquals(emptyList(), result)
    }

    @Test
    fun `maps every field of a normal non-express surcharge`() {
        val result = response(
            appliedCharges = listOf(
                FetchSurchargeResponse.AppliedSurcharge(
                    surchargeDetails = surchargeDetails(applicableOn = "Credit"),
                    calculatedSurchargeFee = 25.5,
                )
            )
        ).toUiModel()

        val surcharge = result.single()
        assertEquals("credit", surcharge.applicableOn) // lowercased applicableOn, not network, for non-express methods
        assertEquals("Card Surcharge", surcharge.title)
        assertEquals("SC1", surcharge.surchargeCode)
        assertEquals("VISA", surcharge.network)
        assertEquals("CONSUMER", surcharge.classification)
        assertEquals(25.5, surcharge.amount)
    }

    @Test
    fun `for googlepay applepay and revolutpay networks, applicableOn is the lowercased network instead`() {
        val result = response(
            appliedCharges = listOf(
                FetchSurchargeResponse.AppliedSurcharge(
                    surchargeDetails = surchargeDetails(network = "GooglePay", applicableOn = "wallet"),
                    calculatedSurchargeFee = 10.0,
                )
            )
        ).toUiModel()

        assertEquals("googlepay", result.single().applicableOn)
    }

    @Test
    fun `missing calculatedSurchargeFee defaults to zero`() {
        val result = response(
            appliedCharges = listOf(
                FetchSurchargeResponse.AppliedSurcharge(surchargeDetails = surchargeDetails(), calculatedSurchargeFee = null)
            )
        ).toUiModel()

        assertEquals(0.0, result.single().amount)
    }

    @Test
    fun `entirely null surchargeDetails maps every text field to an empty string`() {
        val result = response(
            appliedCharges = listOf(
                FetchSurchargeResponse.AppliedSurcharge(surchargeDetails = null, calculatedSurchargeFee = 5.0)
            )
        ).toUiModel()

        val surcharge = result.single()
        assertEquals("", surcharge.applicableOn)
        assertEquals("", surcharge.title)
        assertEquals("", surcharge.surchargeCode)
        assertEquals("", surcharge.network)
        assertEquals("", surcharge.classification)
    }

    @Test
    fun `maps multiple applied surcharges preserving order`() {
        val result = response(
            appliedCharges = listOf(
                FetchSurchargeResponse.AppliedSurcharge(surchargeDetails(title = "First"), 1.0),
                FetchSurchargeResponse.AppliedSurcharge(surchargeDetails(title = "Second"), 2.0),
            )
        ).toUiModel()

        assertEquals(listOf("First", "Second"), result.map { it.title })
    }
}
