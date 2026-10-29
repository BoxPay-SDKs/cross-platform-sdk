package com.crossplatform.sdk.presentation.components.scanner

/**
 * Builds ISO 7816-4 APDU command bytes and parses BER-TLV EMV responses.
 *
 * No external dependencies — runs identically on Android and iOS via the shared source set.
 * All byte values are treated as unsigned (0..255) throughout to match the EMV spec.
 */
internal object EmvParser {

    // ──────────────────────────────────────────────────────────────
    // APDU command builders
    // ──────────────────────────────────────────────────────────────

    /**
     * SELECT PPSE — discovers payment applications on a contactless card.
     * AID = "2PAY.SYS.DDF01"  (Proximity Payment Systems Environment)
     */
    val SELECT_PPSE: ByteArray = byteArrayOf(
        0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(),
        0x0E.toByte(),
        // "2PAY.SYS.DDF01"
        0x32, 0x50, 0x41, 0x59, 0x2E, 0x53, 0x59, 0x53, 0x2E,
        0x44, 0x44, 0x46, 0x30, 0x31,
        0x00.toByte()
    )

    /** SELECT by name — selects the application identified by [aid]. */
    fun buildSelectAid(aid: ByteArray): ByteArray =
        byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte()) + aid + byteArrayOf(0x00)

    /**
     * GET PROCESSING OPTIONS — starts a transaction.
     * Sends an empty PDOL response (83 00) which works for the vast majority of
     * payment cards when we only need to read static data.
     */
    val GET_PROCESSING_OPTIONS: ByteArray = byteArrayOf(
        0x80.toByte(), 0xA8.toByte(), 0x00, 0x00,
        0x02,
        0x83.toByte(), 0x00,  // empty PDOL response
        0x00
    )

    /**
     * READ RECORD — reads record [record] from SFI [sfi].
     * Iterate sfi 1-10, record 1-10 until you find tag 57 or 5A.
     */
    fun buildReadRecord(sfi: Int, record: Int): ByteArray =
        byteArrayOf(0x00, 0xB2.toByte(), record.toByte(), ((sfi shl 3) or 4).toByte(), 0x00)

    // ──────────────────────────────────────────────────────────────
    // Response / TLV helpers
    // ──────────────────────────────────────────────────────────────

    /** Returns true when the last two bytes of an APDU response are 90 00 (success). */
    fun isSuccess(response: ByteArray): Boolean =
        response.size >= 2 &&
                response[response.size - 2] == 0x90.toByte() &&
                response[response.size - 1] == 0x00.toByte()

    /** Strips the trailing 2-byte status word (SW1 SW2) from a response. */
    fun stripSW(response: ByteArray): ByteArray =
        if (response.size >= 2) response.copyOf(response.size - 2) else response

    /**
     * BER-TLV search: finds the value bytes for the first occurrence of [tag] in [data].
     *
     * Handles:
     * - 1-byte tags (tag < 0x1F)
     * - 2-byte tags (first byte ends in 1F)
     * - Constructed (0x20 bit set) tags — recursively searched
     * - Definite short-form lengths (single byte)
     * - Definite long-form lengths (multi-byte, first byte 0x80 | n, then n length bytes)
     */
    fun findTag(data: ByteArray, tag: Int): ByteArray? {
        var i = 0
        while (i < data.size) {
            if (i >= data.size) break

            // --- read tag ---
            val b0 = data[i].toInt() and 0xFF
            val isConstructed = (b0 and 0x20) != 0
            val currentTag: Int
            if ((b0 and 0x1F) == 0x1F) {
                if (i + 1 >= data.size) break
                val b1 = data[i + 1].toInt() and 0xFF
                currentTag = (b0 shl 8) or b1
                i += 2
            } else {
                currentTag = b0
                i += 1
            }

            // --- read length ---
            if (i >= data.size) break
            val lenByte = data[i].toInt() and 0xFF
            i++
            val valueLen: Int
            if (lenByte == 0x80) {
                // Indefinite form — not used in payment cards, skip
                break
            } else if ((lenByte and 0x80) != 0) {
                // Long form
                val numBytes = lenByte and 0x7F
                if (i + numBytes > data.size) break
                var len = 0
                for (k in 0 until numBytes) {
                    len = (len shl 8) or (data[i + k].toInt() and 0xFF)
                }
                i += numBytes
                valueLen = len
            } else {
                valueLen = lenByte
            }

            if (i + valueLen > data.size) break
            val value = data.copyOfRange(i, i + valueLen)

            if (currentTag == tag) return value

            // Search inside constructed (template) tags
            if (isConstructed) {
                val nested = findTag(value, tag)
                if (nested != null) return nested
            }

            i += valueLen
        }
        return null
    }

    /**
     * Collects all AIDs from a PPSE FCI response (tag 6F → A5 → BF0C → 61 → 4F).
     * Returns a list of AID byte arrays, sorted longest-first so a full AID beats a partial.
     */
    fun parseAidsFromPpse(fci: ByteArray): List<ByteArray> {
        val result = mutableListOf<ByteArray>()
        collectTag(fci, 0x4F, result)
        return result.sortedByDescending { it.size }
    }

    private fun collectTag(data: ByteArray, tag: Int, into: MutableList<ByteArray>) {
        var i = 0
        while (i < data.size) {
            if (i >= data.size) break
            val b0 = data[i].toInt() and 0xFF
            val isConstructed = (b0 and 0x20) != 0
            val currentTag: Int
            if ((b0 and 0x1F) == 0x1F) {
                if (i + 1 >= data.size) break
                val b1 = data[i + 1].toInt() and 0xFF
                currentTag = (b0 shl 8) or b1
                i += 2
            } else {
                currentTag = b0
                i++
            }
            if (i >= data.size) break
            val lenByte = data[i].toInt() and 0xFF
            i++
            val valueLen: Int = when {
                lenByte == 0x80 -> break
                (lenByte and 0x80) != 0 -> {
                    val n = lenByte and 0x7F
                    if (i + n > data.size) break
                    var len = 0
                    for (k in 0 until n) len = (len shl 8) or (data[i + k].toInt() and 0xFF)
                    i += n
                    len
                }
                else -> lenByte
            }
            if (i + valueLen > data.size) break
            val value = data.copyOfRange(i, i + valueLen)
            if (currentTag == tag) into.add(value)
            if (isConstructed) collectTag(value, tag, into)
            i += valueLen
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Card data extraction
    // ──────────────────────────────────────────────────────────────

    /**
     * Parses Track 2 Equivalent Data (EMV tag 57) into a [ScannedCard].
     *
     * Track 2 BCD format:
     *   [PAN] D [YYMM] [ServiceCode3digits] [DiscretionaryData] F?
     *
     * The bytes are BCD-encoded and the whole field is represented as a hex string
     * where the separator is the character 'D'.
     */
    fun parseTrack2(track2Bytes: ByteArray): ScannedCard? {
        // Convert raw bytes → hex nibble string (each byte = 2 hex chars)
        val hex = track2Bytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }

        // The separator between PAN and expiry is the nibble 'D'
        val dIdx = hex.indexOf('D')
        if (dIdx < 0) return null

        val pan = hex.substring(0, dIdx)
        if (pan.length < 13 || pan.length > 19) return null
        if (!pan.all { it.isDigit() }) return null
        if (!CardTextParser.isLuhnValid(pan)) return null

        val afterD = hex.substring(dIdx + 1)
        if (afterD.length < 4) return ScannedCard(cardNumber = pan)

        val yy = afterD.substring(0, 2)
        val mm = afterD.substring(2, 4)
        if (!yy.all { it.isDigit() } || !mm.all { it.isDigit() }) return ScannedCard(cardNumber = pan)
        val month = mm.toIntOrNull() ?: return ScannedCard(cardNumber = pan)
        if (month !in 1..12) return ScannedCard(cardNumber = pan)

        return ScannedCard(cardNumber = pan, expiryMonth = mm, expiryYear = yy)
    }

    /**
     * Parses a raw PAN from tag 5A (BCD, right-padded with 0xF nibbles).
     */
    fun parsePanTag5A(panBytes: ByteArray): String? {
        val hex = panBytes.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }
        // Strip trailing F padding nibble
        val pan = hex.trimEnd('F', 'f')
        if (pan.length !in 13..19) return null
        if (!pan.all { it.isDigit() }) return null
        if (!CardTextParser.isLuhnValid(pan)) return null
        return pan
    }
}
