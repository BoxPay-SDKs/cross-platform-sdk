package com.crossplatform.sdk.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ReceiptTextTest {

    // Fixed-width font: every character is 10 units wide.
    private val measure: (String) -> Double = { it.length * 10.0 }

    // ── productNameLines ─────────────────────────────────────────────────────

    @Test
    fun `single product stays on one line without a comma`() {
        assertEquals(listOf("Latte"), productNameLines("Latte"))
    }

    @Test
    fun `each comma separated product goes on its own line`() {
        assertEquals(
            listOf("ProductName1,", "ProductName2,", "ProductName3"),
            productNameLines("ProductName1, ProductName2, ProductName3")
        )
    }

    @Test
    fun `spaces around commas are trimmed`() {
        assertEquals(listOf("A,", "B"), productNameLines("  A ,B  "))
    }

    @Test
    fun `empty entries between commas are dropped`() {
        assertEquals(listOf("A,", "B"), productNameLines("A,, ,B,"))
    }

    @Test
    fun `product names keep their inner spaces`() {
        assertEquals(listOf("Cold Brew,", "Iced Latte"), productNameLines("Cold Brew, Iced Latte"))
    }

    @Test
    fun `blank or comma only input is returned unchanged as one line`() {
        assertEquals(listOf(""), productNameLines(""))
        assertEquals(listOf(","), productNameLines(","))
    }

    // ── ellipsize ────────────────────────────────────────────────────────────

    @Test
    fun `text that fits is returned unchanged`() {
        assertEquals("Latte", ellipsize("Latte", 100.0, measure))
        assertEquals("abcdefghij", ellipsize("abcdefghij", 100.0, measure))
    }

    @Test
    fun `text that is too wide is cut and ends with an ellipsis within the width`() {
        val result = ellipsize("Extra Large Caramel Macchiato", 100.0, measure)

        assertEquals("Extra Lar\u2026", result)
        assertEquals(true, measure(result) <= 100.0)
    }

    @Test
    fun `trailing space before the ellipsis is trimmed`() {
        assertEquals("Extra\u2026", ellipsize("Extra Large", 70.0, measure))
    }

    @Test
    fun `width too small for any character returns just the ellipsis`() {
        assertEquals("\u2026", ellipsize("Latte", 5.0, measure))
    }
}
