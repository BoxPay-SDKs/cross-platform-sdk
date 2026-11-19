package com.crossplatform.sdk.presentation.components

import com.crossplatform.sdk.domain.model.SelectedPaymentMethod

internal const val NICKNAME_MAX_LENGTH = 30
private const val CARD_NUMBER_MIN_DIGITS = 13
private val ALLOWED_NICKNAME_SYMBOLS = setOf(' ', '.', '_', '\'', '-')

internal enum class NicknameError { EMPTY, TOO_LONG, INVALID_CHARACTERS, LOOKS_LIKE_CARD_NUMBER }

/**
 * Mirrors the backend rules for a saved card nickname: not blank, at most
 * [NICKNAME_MAX_LENGTH] characters, only letters, digits, spaces and . _ ' -,
 * and not something that looks like a card number (13 or more digits).
 * Returns null when the nickname is valid.
 */
internal fun validateNickname(nickName: String): NicknameError? {
    val trimmed = nickName.trim()
    if (trimmed.isEmpty()) return NicknameError.EMPTY
    if (trimmed.length > NICKNAME_MAX_LENGTH) return NicknameError.TOO_LONG
    if (trimmed.any { !it.isLetterOrDigit() && it !in ALLOWED_NICKNAME_SYMBOLS }) {
        return NicknameError.INVALID_CHARACTERS
    }
    val compact = trimmed.filterNot { it == ' ' || it == '-' }
    if (compact.length >= CARD_NUMBER_MIN_DIGITS && compact.all { it.isDigit() }) {
        return NicknameError.LOOKS_LIKE_CARD_NUMBER
    }
    return null
}

internal fun List<SelectedPaymentMethod>.withDisplayName(id: String, displayName: String): List<SelectedPaymentMethod> =
    map { if (it.id == id) it.copy(displayName = displayName) else it }
