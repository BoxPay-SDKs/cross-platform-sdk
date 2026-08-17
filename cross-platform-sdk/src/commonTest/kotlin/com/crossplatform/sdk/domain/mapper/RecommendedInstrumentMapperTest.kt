package com.crossplatform.sdk.domain.mapper

import com.crossplatform.sdk.data.model.RecommendedInstrumentsResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RecommendedInstrumentMapperTest {

    private fun instrument(
        type: String? = "Card",
        instrumentRef: String? = "ref_1",
        displayValue: String? = "•••• 1111",
        logoUrl: String? = "/assets/visa.png",
        cardNickName: String? = null,
    ) = RecommendedInstrumentsResponse(
        type = type,
        brand = "VISA",
        instrumentRef = instrumentRef,
        displayValue = displayValue,
        logoUrl = logoUrl,
        cardNickName = cardNickName,
    )

    @Test
    fun `displayName prefers the card nickname over the raw display value`() {
        val result = listOf(instrument(cardNickName = "My Shopping Card", displayValue = "•••• 1111")).toUiModel()

        assertEquals("My Shopping Card", result.single().displayName)
    }

    @Test
    fun `displayName falls back to displayValue when there is no nickname`() {
        val result = listOf(instrument(cardNickName = null, displayValue = "•••• 1111")).toUiModel()

        assertEquals("•••• 1111", result.single().displayName)
    }

    @Test
    fun `displayName is empty when both nickname and displayValue are null`() {
        val result = listOf(instrument(cardNickName = null, displayValue = null)).toUiModel()

        assertEquals("", result.single().displayName)
    }

    @Test
    fun `id and instrumentType both come from instrumentRef`() {
        val result = listOf(instrument(instrumentRef = "ref_42")).toUiModel()

        assertEquals("ref_42", result.single().id)
        assertEquals("ref_42", result.single().instrumentType)
    }

    @Test
    fun `missing logoUrl falls back to the triple-slash placeholder, not empty string`() {
        // This mirrors the mapper's actual `?: "///"` default exactly —
        // worth calling out explicitly since it's a slightly unusual
        // default that a future refactor might accidentally "fix" to "".
        val result = listOf(instrument(logoUrl = null)).toUiModel()

        assertEquals("///", result.single().imageUrl)
    }

    @Test
    fun `type defaults to an empty string when missing`() {
        val result = listOf(instrument(type = null)).toUiModel()

        assertEquals("", result.single().type)
    }

    @Test
    fun `every mapped instrument starts unselected and not marked last used`() {
        val result = listOf(instrument()).toUiModel()

        assertFalse(result.single().isSelected)
        assertFalse(result.single().isLastUsed)
    }
}
