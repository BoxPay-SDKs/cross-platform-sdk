package com.crossplatform.sdk.presentation.components.scanner

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal fun hex(s: String): ByteArray {
    val clean = s.replace(" ", "")
    return ByteArray(clean.length / 2) { clean.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}

/** BER-TLV encoder used to build fake card responses. */
internal fun tlv(tagHex: String, value: ByteArray): ByteArray {
    val len = value.size
    val lenBytes = when {
        len < 0x80  -> byteArrayOf(len.toByte())
        len < 0x100 -> byteArrayOf(0x81.toByte(), len.toByte())
        else        -> byteArrayOf(0x82.toByte(), (len shr 8).toByte(), len.toByte())
    }
    return hex(tagHex) + lenBytes + value
}

class EmvParserTest {

    // ── TLV search ───────────────────────────────────────────────────────────

    @Test
    fun findTag_shortFormAndNested() {
        val inner = tlv("5A", hex("4111111111111111"))
        val data  = tlv("70", tlv("61", inner))
        assertContentEquals(hex("4111111111111111"), EmvParser.findTag(data, 0x5A))
        assertNull(EmvParser.findTag(data, 0x57))
    }

    @Test
    fun findTag_twoByteTag() {
        val data = tlv("6F", tlv("A5", tlv("9F38", hex("9F6604"))))
        assertContentEquals(hex("9F6604"), EmvParser.findTag(data, 0x9F38))
    }

    @Test
    fun findTag_longFormLengths() {
        val v200 = ByteArray(200) { 7 }
        val v300 = ByteArray(300) { 9 }
        assertContentEquals(v200, EmvParser.findTag(tlv("70", tlv("5F20", v200)), 0x5F20))
        assertContentEquals(v300, EmvParser.findTag(tlv("77", tlv("94", v300)), 0x94))
    }

    @Test
    fun findTag_truncatedDataDoesNotCrash() {
        assertNull(EmvParser.findTag(hex("70 20 5A 08 41"), 0x5A))
        assertNull(EmvParser.findTag(ByteArray(0), 0x5A))
    }

    @Test
    fun parseAidsFromPpse_longestFirst() {
        val app1 = tlv("61", tlv("4F", hex("A0000000031010")))
        val app2 = tlv("61", tlv("4F", hex("A00000002501")))
        val fci  = tlv("6F", tlv("A5", tlv("BF0C", app1 + app2)))
        val aids = EmvParser.parseAidsFromPpse(fci)
        assertEquals(2, aids.size)
        assertContentEquals(hex("A0000000031010"), aids[0])
    }

    // ── status words ─────────────────────────────────────────────────────────

    @Test
    fun isSuccess_andStripSW() {
        assertTrue(EmvParser.isSuccess(hex("AABB9000")))
        assertFalse(EmvParser.isSuccess(hex("AABB6985")))
        assertFalse(EmvParser.isSuccess(hex("90")))
        assertContentEquals(hex("AABB"), EmvParser.stripSW(hex("AABB9000")))
    }

    // ── DOL / PDOL ───────────────────────────────────────────────────────────

    @Test
    fun parseDol_handlesMultiByteTags() {
        // 9F66 04 | 9F02 06 | 5F2A 02 | 95 05 | 9A 03
        val dol = parseDolHex("9F6604 9F0206 5F2A02 9505 9A03")
        assertEquals(
            listOf(0x9F66 to 4, 0x9F02 to 6, 0x5F2A to 2, 0x95 to 5, 0x9A to 3),
            dol
        )
    }

    private fun parseDolHex(s: String) = EmvParser.parseDol(hex(s))

    @Test
    fun buildPdolData_totalLengthMatchesAndKnownTagsAreFilled() {
        val pdol = hex("9F6604 9F0206 9F1A02 9505 9C01 9A03 5F2A02")
        val data = EmvParser.buildPdolData(pdol)
        assertEquals(4 + 6 + 2 + 5 + 1 + 3 + 2, data.size)

        assertContentEquals(hex("27000000"), data.copyOfRange(0, 4))          // TTQ
        assertContentEquals(hex("000000000000"), data.copyOfRange(4, 10))     // amount
        assertContentEquals(hex("0356"), data.copyOfRange(10, 12))            // country
        assertContentEquals(hex("0000000000"), data.copyOfRange(12, 17))      // TVR
        assertContentEquals(hex("00"), data.copyOfRange(17, 18))              // txn type
        assertContentEquals(hex("0356"), data.copyOfRange(21, 23))            // currency
    }

    @Test
    fun buildPdolData_unknownTagsAreZeroFilledAndLengthsAreRespected() {
        val data = EmvParser.buildPdolData(hex("DF8101 05 9F1A03"))
        // DF8101 is a 3-byte tag (DF 81 01) with length 05, then 9F1A with a non-standard length 3
        assertContentEquals(hex("0000000000" + "000356"), data)
    }

    @Test
    fun buildPdolData_unpredictableNumberHasRequestedLength() {
        assertEquals(4, EmvParser.buildPdolData(hex("9F3704")).size)
    }

    @Test
    fun buildGetProcessingOptions_emptyPdol() {
        assertContentEquals(hex("80A8000002830000"), EmvParser.buildGetProcessingOptions(null))
        assertContentEquals(hex("80A8000002830000"), EmvParser.buildGetProcessingOptions(ByteArray(0)))
    }

    @Test
    fun buildGetProcessingOptions_withPdol() {
        val cmd = EmvParser.buildGetProcessingOptions(hex("9F6604"))
        // CLA INS P1 P2 Lc | 83 04 <TTQ> | Le
        assertContentEquals(hex("80A8000006" + "8304" + "27000000" + "00"), cmd)
    }

    @Test
    fun buildGetProcessingOptions_oversizedPdolFallsBackToEmpty() {
        val huge = hex("9F0206".repeat(50))   // asks for 300 bytes of terminal data
        assertContentEquals(hex("80A8000002830000"), EmvParser.buildGetProcessingOptions(huge))
    }

    // ── command builders ─────────────────────────────────────────────────────

    @Test
    fun buildSelectAid_andReadRecord() {
        assertContentEquals(hex("00A4040007A000000003101000"), EmvParser.buildSelectAid(hex("A0000000031010")))
        // SFI 2, record 3 → P2 = (2 << 3) | 4 = 0x14
        assertContentEquals(hex("00B2031400"), EmvParser.buildReadRecord(2, 3))
    }

    // ── card data ────────────────────────────────────────────────────────────

    @Test
    fun parseTrack2_validCard() {
        val card = EmvParser.parseTrack2(hex("4111111111111111D281220100"))
        assertNotNull(card)
        assertEquals("4111111111111111", card.cardNumber)
        assertEquals("12", card.expiryMonth)
        assertEquals("28", card.expiryYear)
    }

    @Test
    fun parseTrack2_invalidMonthKeepsPanDropsExpiry() {
        val card = EmvParser.parseTrack2(hex("4111111111111111D281320100"))
        assertNotNull(card)
        assertEquals("4111111111111111", card.cardNumber)
        assertNull(card.expiryMonth)
        assertNull(card.expiryYear)
    }

    @Test
    fun parseTrack2_rejectsBadLuhnMissingSeparatorAndBadLength() {
        assertNull(EmvParser.parseTrack2(hex("4111111111111112D281220100")))   // Luhn fails
        assertNull(EmvParser.parseTrack2(hex("411111111111111128122010")))      // no 'D'
        assertNull(EmvParser.parseTrack2(hex("411111D281220100")))              // PAN too short
    }

    @Test
    fun parsePanTag5A_stripsFPadding() {
        assertEquals("378282246310005", EmvParser.parsePanTag5A(hex("378282246310005F")))
        assertEquals("4111111111111111", EmvParser.parsePanTag5A(hex("4111111111111111")))
        assertNull(EmvParser.parsePanTag5A(hex("4111111111111112")))            // Luhn fails
    }
}
