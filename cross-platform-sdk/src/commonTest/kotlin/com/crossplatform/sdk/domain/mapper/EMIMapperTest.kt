package com.crossplatform.sdk.domain.mapper

import com.crossplatform.sdk.data.model.ApplicableOffer
import com.crossplatform.sdk.data.model.Discount
import com.crossplatform.sdk.data.model.EmiMethod
import com.crossplatform.sdk.data.model.PaymentMethod
import com.crossplatform.sdk.data.model.ProcessingFee
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [EMIMapperTest] verifies `List<PaymentMethod>.toUiModel()`, the pure
 * function that turns the raw session-details EMI payment methods into the
 * grouped [com.crossplatform.sdk.domain.model.ChooseEmiModel] the "Choose
 * EMI" screen renders.
 */
class EMIMapperTest {

    private fun emiMethod(
        duration: Int = 3,
        effectiveInterestRate: Double = 12.0,
        merchantBorneInterestRate: Double = 0.0,
        issuerTitle: String? = "HDFC Bank",
        cardlessEmiProviderTitle: String? = null,
        cardlessEmiProviderValue: String? = null,
        issuer: String? = "HDFC",
        emiAmountLocaleFull: String = "₹1,700",
        totalAmountLocaleFull: String = "₹5,100",
        netAmountLocaleFull: String = "₹5,000",
        merchantBorneInterestAmountLocaleFull: String = "₹0",
        bankChargedInterestAmountLocaleFull: String = "₹100",
        interestChargedAmountLocaleFull: String = "₹50",
        processingFeeAmount: String? = "₹0",
    ) = EmiMethod(
        duration = duration,
        effectiveInterestRate = effectiveInterestRate,
        merchantBorneInterestRate = merchantBorneInterestRate,
        issuerTitle = issuerTitle,
        issuer = issuer,
        processingFee = ProcessingFee(amountLocaleFull = processingFeeAmount),
        netAmountLocaleFull = netAmountLocaleFull,
        totalAmountLocaleFull = totalAmountLocaleFull,
        emiAmountLocaleFull = emiAmountLocaleFull,
        merchantBorneInterestAmountLocaleFull = merchantBorneInterestAmountLocaleFull,
        bankChargedInterestAmountLocaleFull = bankChargedInterestAmountLocaleFull,
        interestChargedAmountLocaleFull = interestChargedAmountLocaleFull,
        cardlessEmiProviderTitle = cardlessEmiProviderTitle,
        cardlessEmiProviderValue = cardlessEmiProviderValue,
    )

    private fun paymentMethod(
        title: String? = "Credit Card EMI",
        type: String = "Emi",
        emiMethod: EmiMethod? = emiMethod(),
        logoUrl: String? = "/assets/hdfc.png",
        offerDiscountType: String? = null,
        offerCode: String? = null,
    ) = PaymentMethod(
        id = "pm_1",
        type = type,
        brand = "Emi",
        title = title,
        logoUrl = logoUrl,
        emiMethod = emiMethod,
        applicableOffer = offerDiscountType?.let {
            listOf(ApplicableOffer(code = offerCode ?: "OFFER1", title = "Offer", discount = Discount(type = it)))
        }
    )

    @Test
    fun `non-EMI payment methods are ignored`() {
        val methods = listOf(
            paymentMethod(type = "Card", emiMethod = null),
            paymentMethod(type = "Upi", emiMethod = null),
        )

        val result = methods.toUiModel()

        assertTrue(result.cards.isEmpty())
    }

    @Test
    fun `payment method with Emi type but no emiMethod payload is ignored`() {
        val methods = listOf(paymentMethod(type = "Emi", emiMethod = null))

        val result = methods.toUiModel()

        assertTrue(result.cards.isEmpty())
    }

    @Test
    fun `title containing Credit is grouped under Credit Card`() {
        val methods = listOf(paymentMethod(title = "HDFC Credit Card EMI"))

        val result = methods.toUiModel()

        assertEquals(listOf("Credit Card"), result.cards.map { it.cardType })
    }

    @Test
    fun `title containing Debit is grouped under Debit Card`() {
        val methods = listOf(paymentMethod(title = "HDFC Debit Card EMI"))

        val result = methods.toUiModel()

        assertEquals(listOf("Debit Card"), result.cards.map { it.cardType })
    }

    @Test
    fun `title without Credit or Debit falls back to Others and uses cardless provider fields`() {
        val methods = listOf(
            paymentMethod(
                title = "Bajaj Finserv EMI",
                emiMethod = emiMethod(
                    issuerTitle = "Should not be used",
                    cardlessEmiProviderTitle = "Bajaj Finserv",
                    cardlessEmiProviderValue = "BAJAJ",
                )
            )
        )

        val result = methods.toUiModel()

        assertEquals("Others", result.cards.single().cardType)
        val bank = result.cards.single().banks.single()
        assertEquals("Bajaj Finserv", bank.name)
        assertEquals("BAJAJ", bank.cardLessEmiValue)
        assertEquals("", bank.issuerBrand) // issuer brand is blanked out for "Others"
        assertEquals(0.0, bank.percent) // interest rate is forced to 0 for cardless/Others
    }

    @Test
    fun `cards are ordered Credit then Debit then Others regardless of input order`() {
        val methods = listOf(
            paymentMethod(title = "Other Provider EMI"),
            paymentMethod(title = "Debit Card EMI"),
            paymentMethod(title = "Credit Card EMI"),
        )

        val result = methods.toUiModel()

        assertEquals(listOf("Credit Card", "Debit Card", "Others"), result.cards.map { it.cardType })
    }

    @Test
    fun `NoCostEmi offer uses merchant borne interest rate and marks noCostApplied`() {
        val methods = listOf(
            paymentMethod(
                emiMethod = emiMethod(effectiveInterestRate = 14.0, merchantBorneInterestRate = 0.0),
                offerDiscountType = "NoCostEmi",
            )
        )

        val emi = result(methods).single()

        assertTrue(emi.noCostApplied)
        assertEquals(0.0, emi.percent) // merchantBorneInterestRate, not the bank's effective rate
    }

    @Test
    fun `LowCostEmi offer uses interestChargedAmountLocaleFull for interest charged`() {
        val methods = listOf(
            paymentMethod(
                emiMethod = emiMethod(interestChargedAmountLocaleFull = "₹25"),
                offerDiscountType = "LowCostEmi",
            )
        )

        val emi = result(methods).single()

        assertTrue(emi.lowCostApplied)
        assertEquals("₹25", emi.interestCharged)
    }

    @Test
    fun `regular EMI with no offer uses bank charged interest amount`() {
        val methods = listOf(
            paymentMethod(emiMethod = emiMethod(bankChargedInterestAmountLocaleFull = "₹120"))
        )

        val emi = result(methods).single()

        assertEquals(false, emi.noCostApplied)
        assertEquals(false, emi.lowCostApplied)
        assertEquals("₹120", emi.interestCharged)
    }

    @Test
    fun `duplicate bank same duration is de-duplicated, not appended twice`() {
        val methods = listOf(
            paymentMethod(emiMethod = emiMethod(duration = 3)),
            paymentMethod(emiMethod = emiMethod(duration = 3)),
        )

        val model = methods.toUiModel()
        val banks = model.cards.single().banks

        assertEquals(1, banks.size)
        assertEquals(1, banks.single().emiList.size)
    }

    @Test
    fun `same bank different duration is appended as a second tenure option`() {
        val methods = listOf(
            paymentMethod(emiMethod = emiMethod(duration = 3)),
            paymentMethod(emiMethod = emiMethod(duration = 6)),
        )

        val model = methods.toUiModel()
        val banks = model.cards.single().banks

        assertEquals(1, banks.size)
        assertEquals(listOf(3, 6), banks.single().emiList.map { it.duration }.sorted())
    }

    @Test
    fun `emiList within a bank is sorted noCost first then lowCost then by duration ascending`() {
        val methods = listOf(
            paymentMethod(emiMethod = emiMethod(duration = 12), offerDiscountType = null),
            paymentMethod(emiMethod = emiMethod(duration = 6), offerDiscountType = "LowCostEmi"),
            paymentMethod(emiMethod = emiMethod(duration = 3), offerDiscountType = "NoCostEmi"),
        )

        val model = methods.toUiModel()
        val emis = model.cards.single().banks.single().emiList

        assertEquals(listOf(3, 6, 12), emis.map { it.duration })
    }

    @Test
    fun `logoUrl with relative assets path is prefixed with the checkout host`() {
        val methods = listOf(paymentMethod(logoUrl = "/assets/hdfc.png"))

        val model = methods.toUiModel()

        assertEquals(
            "https://checkout.boxpay.in/assets/hdfc.png",
            model.cards.single().banks.single().iconUrl
        )
    }

    @Test
    fun `logoUrl that is already absolute is left untouched`() {
        val methods = listOf(paymentMethod(logoUrl = "https://cdn.example.com/hdfc.png"))

        val model = methods.toUiModel()

        assertEquals("https://cdn.example.com/hdfc.png", model.cards.single().banks.single().iconUrl)
    }

    @Test
    fun `missing logoUrl defaults to empty string`() {
        val methods = listOf(paymentMethod(logoUrl = null))

        val model = methods.toUiModel()

        assertEquals("", model.cards.single().banks.single().iconUrl)
    }

    private fun result(methods: List<PaymentMethod>) =
        methods.toUiModel().cards.single().banks.single().emiList
}
