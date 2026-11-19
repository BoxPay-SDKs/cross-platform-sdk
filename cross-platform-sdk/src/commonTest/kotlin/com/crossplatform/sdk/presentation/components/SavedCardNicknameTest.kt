package com.crossplatform.sdk.presentation.components

import com.crossplatform.sdk.domain.model.SelectedPaymentMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SavedCardNicknameTest {

    @Test
    fun `a normal nickname is valid`() {
        assertNull(validateNickname("My Visa"))
    }

    @Test
    fun `allowed symbols are accepted`() {
        assertNull(validateNickname("Dad's card_1 - work."))
    }

    @Test
    fun `digits in a nickname are accepted`() {
        assertNull(validateNickname("Card 2026"))
    }

    @Test
    fun `blank nickname is rejected`() {
        assertEquals(NicknameError.EMPTY, validateNickname(""))
        assertEquals(NicknameError.EMPTY, validateNickname("   "))
    }

    @Test
    fun `nickname of exactly 30 characters is valid and 31 is too long`() {
        assertNull(validateNickname("a".repeat(30)))
        assertEquals(NicknameError.TOO_LONG, validateNickname("a".repeat(31)))
    }

    @Test
    fun `length is checked after trimming`() {
        assertNull(validateNickname("  " + "a".repeat(30) + "  "))
    }

    @Test
    fun `characters outside letters digits space and dot underscore apostrophe hyphen are rejected`() {
        listOf("Card#1", "My@card", "a,b", "50% off", "card!", "x/y", "emoji\uD83D\uDE00").forEach {
            assertEquals(NicknameError.INVALID_CHARACTERS, validateNickname(it), it)
        }
    }

    @Test
    fun `13 or more digits look like a card number`() {
        assertEquals(NicknameError.LOOKS_LIKE_CARD_NUMBER, validateNickname("4111111111111"))
        assertEquals(NicknameError.LOOKS_LIKE_CARD_NUMBER, validateNickname("4111111111111111"))
    }

    @Test
    fun `card number with spaces or hyphens still looks like a card number`() {
        assertEquals(NicknameError.LOOKS_LIKE_CARD_NUMBER, validateNickname("4111 1111 1111 1111"))
        assertEquals(NicknameError.LOOKS_LIKE_CARD_NUMBER, validateNickname("4111-1111-1111-1111"))
    }

    @Test
    fun `12 digits do not look like a card number`() {
        assertNull(validateNickname("411111111111"))
    }

    @Test
    fun `digits mixed with letters are not treated as a card number`() {
        assertNull(validateNickname("Visa 4111111111111"))
    }

    // ── withDisplayName ──────────────────────────────────────────────────────

    private fun card(id: String, name: String) = SelectedPaymentMethod(
        type = "Card", id = id, displayName = name, displayValue = "•••• 1111",
        imageUrl = "", instrumentType = id, isLastUsed = null, isSelected = null,
    )

    @Test
    fun `withDisplayName renames only the matching instrument`() {
        val result = listOf(card("a", "Old A"), card("b", "Old B")).withDisplayName("a", "New A")

        assertEquals(listOf("New A", "Old B"), result.map { it.displayName })
    }

    @Test
    fun `withDisplayName leaves the list unchanged when the id is unknown`() {
        val list = listOf(card("a", "Old A"))

        assertEquals(list, list.withDisplayName("zzz", "New"))
    }
}
