package com.crossplatform.sdk.domain.mapper

import com.crossplatform.sdk.data.model.PaymentMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [SelectedPaymentMethodMapperTest] covers `List<PaymentMethod>.toUiModel(type: String)`,
 * used by the wallet/netbanking/BNPL selection screens to filter the raw
 * session payment methods down to just the ones for that screen's type.
 */
class SelectedPaymentMethodMapperTest {

    private fun paymentMethod(
        type: String = "Wallet",
        title: String? = "Google Pay",
        id: String = "pm_1",
        logoUrl: String? = "/assets/gpay.png",
        instrumentTypeValue: String? = "googlepay",
    ) = PaymentMethod(id = id, type = type, brand = type, title = title, logoUrl = logoUrl, instrumentTypeValue = instrumentTypeValue)

    @Test
    fun `filters out methods whose type does not match, case-insensitively`() {
        val methods = listOf(
            paymentMethod(type = "Wallet", title = "Google Pay"),
            paymentMethod(type = "WALLET", title = "Apple Pay"),
            paymentMethod(type = "NetBanking", title = "HDFC"),
        )

        val result = methods.toUiModel(type = "wallet")

        assertEquals(setOf("Apple Pay", "Google Pay"), result.map { it.displayName }.toSet())
    }

    @Test
    fun `drops entries whose title is blank after trimming`() {
        val methods = listOf(
            paymentMethod(title = "   "),
            paymentMethod(title = "Google Pay"),
        )

        val result = methods.toUiModel(type = "Wallet")

        assertEquals(listOf("Google Pay"), result.map { it.displayName })
    }

    @Test
    fun `keeps entries with a null title mapped to an empty display name`() {
        val methods = listOf(paymentMethod(title = null))

        val result = methods.toUiModel(type = "Wallet")

        assertEquals("", result.single().displayName)
    }

    @Test
    fun `results are sorted alphabetically by display name, case-insensitively`() {
        val methods = listOf(
            paymentMethod(title = "zoho pay"),
            paymentMethod(title = "Apple Pay"),
            paymentMethod(title = "Google Pay"),
        )

        val result = methods.toUiModel(type = "Wallet")

        assertEquals(listOf("Apple Pay", "Google Pay", "zoho pay"), result.map { it.displayName })
    }

    @Test
    fun `every result starts unselected and not marked as last used`() {
        val result = listOf(paymentMethod()).toUiModel(type = "Wallet")

        assertTrue(result.none { it.isSelected })
        assertTrue(result.none { it.isLastUsed })
    }

    @Test
    fun `missing logoUrl and instrumentTypeValue default to empty strings`() {
        val result = listOf(paymentMethod(logoUrl = null, instrumentTypeValue = null)).toUiModel(type = "Wallet")

        assertEquals("", result.single().imageUrl)
        assertEquals("", result.single().instrumentType)
    }
}
