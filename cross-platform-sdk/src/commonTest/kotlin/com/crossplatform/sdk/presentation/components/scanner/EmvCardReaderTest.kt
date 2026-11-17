package com.crossplatform.sdk.presentation.components.scanner

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EmvCardReaderTest {

    private val sw9000 = hex("9000")
    private val visaAid = hex("A0000000031010")

    private fun ppse(vararg aids: ByteArray): ByteArray {
        val apps = aids.fold(ByteArray(0)) { acc, aid -> acc + tlv("61", tlv("4F", aid)) }
        return tlv("6F", tlv("84", hex("325041592E5359532E4444463031")) + tlv("A5", tlv("BF0C", apps))) + sw9000
    }

    private fun selectResponse(pdol: ByteArray?): ByteArray {
        val a5 = tlv("50", "VISA".encodeToByteArray()) + (if (pdol != null) tlv("9F38", pdol) else ByteArray(0))
        return tlv("6F", tlv("84", visaAid) + tlv("A5", a5)) + sw9000
    }

    /** GPO answer in format 2 (tag 77) with AIP + AFL. */
    private fun gpoFormat2(afl: ByteArray) = tlv("77", tlv("82", hex("2000")) + tlv("94", afl)) + sw9000

    private fun record(track2: String) = tlv("70", tlv("57", hex(track2))) + sw9000

    /** A scripted card. [commands] records every APDU the reader sent. */
    private class FakeCard(
        private val ppseResp: ByteArray?,
        private val selectResp: ByteArray?,
        private val gpoResp: ByteArray?,
        private val records: Map<String, ByteArray> = emptyMap()
    ) {
        val commands = mutableListOf<ByteArray>()

        private fun byteHex(b: Byte) = (b.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase()

        fun transceive(apdu: ByteArray): ByteArray? {
            commands += apdu
            val ins = apdu[1].toInt() and 0xFF
            return when {
                apdu.contentEquals(EmvParser.SELECT_PPSE) -> ppseResp
                ins == 0xA4 -> selectResp
                ins == 0xA8 -> gpoResp
                ins == 0xB2 -> records[byteHex(apdu[2]) + byteHex(apdu[3])]
                    ?: hex("6A83")
                else -> null
            }
        }
    }

    @Test
    fun read_sendsPdolDataAndReturnsCardFromTrack2() {
        val card = FakeCard(
            ppseResp = ppse(visaAid),
            selectResp = selectResponse(pdol = hex("9F6604 9F0206")),
            gpoResp = gpoFormat2(hex("08010100")),          // SFI 1, record 1..1
            records = mapOf("010C" to record("4111111111111111D281220100"))
        )

        val result = EmvCardReader.read(card::transceive)

        assertNotNull(result)
        assertEquals("4111111111111111", result.cardNumber)
        assertEquals("12", result.expiryMonth)
        assertEquals("28", result.expiryYear)

        // GPO must carry TTQ + zero amount: 83 0A 27000000 000000000000
        val gpo = card.commands.first { (it[1].toInt() and 0xFF) == 0xA8 }
        assertContentEquals(hex("80A800000C" + "830A" + "27000000" + "000000000000" + "00"), gpo)
    }

    @Test
    fun read_withoutPdolSendsEmptyGpo() {
        val card = FakeCard(
            ppseResp = ppse(visaAid),
            selectResp = selectResponse(pdol = null),
            gpoResp = gpoFormat2(hex("08010100")),
            records = mapOf("010C" to record("4111111111111111D281220100"))
        )
        assertNotNull(EmvCardReader.read(card::transceive))
        val gpo = card.commands.first { (it[1].toInt() and 0xFF) == 0xA8 }
        assertContentEquals(hex("80A8000002830000"), gpo)
    }

    @Test
    fun read_failedGpoStillFallsBackToCommonRecords() {
        val card = FakeCard(
            ppseResp = ppse(visaAid),
            selectResp = selectResponse(pdol = hex("9F6604")),
            gpoResp = hex("6985"),
            records = mapOf("0214" to record("4111111111111111D281220100"))   // SFI 2, record 2 (P2 = 2<<3 or 4 = 0x14)
        )
        val result = EmvCardReader.read(card::transceive)
        assertNotNull(result)
        assertEquals("4111111111111111", result.cardNumber)
    }

    @Test
    fun read_triesNextAidWhenSelectFails() {
        var selects = 0
        val good = selectResponse(null)
        val card = FakeCard(
            ppseResp = ppse(visaAid, hex("A00000002501")),
            selectResp = null,
            gpoResp = gpoFormat2(hex("08010100")),
            records = mapOf("010C" to record("4111111111111111D281220100"))
        )
        val result = EmvCardReader.read { apdu ->
            if ((apdu[1].toInt() and 0xFF) == 0xA4 && !apdu.contentEquals(EmvParser.SELECT_PPSE)) {
                selects++
                if (selects == 1) hex("6A82") else good      // first AID refused, second accepted
            } else card.transceive(apdu)
        }
        assertNotNull(result)
        assertEquals(2, selects)
    }

    @Test
    fun read_returnsNullWhenPpseFailsOrCardGoesAway() {
        assertNull(EmvCardReader.read { null })
        assertNull(EmvCardReader.read { hex("6A82") })
        val noAids = tlv("6F", tlv("A5", tlv("BF0C", ByteArray(0)))) + sw9000
        assertNull(EmvCardReader.read { if (it.contentEquals(EmvParser.SELECT_PPSE)) noAids else null })
    }

    @Test
    fun read_returnsNullWhenNoRecordHasCardData() {
        val card = FakeCard(ppse(visaAid), selectResponse(null), gpoFormat2(hex("08010200")))
        assertNull(EmvCardReader.read(card::transceive))
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    @Test
    fun extractAfl_format2AndFormat1() {
        val afl = hex("08010100 10010300")
        assertContentEquals(afl, EmvCardReader.extractAfl(tlv("77", tlv("94", afl))))
        // format 1: 80 len <AIP 2 bytes><AFL>
        assertContentEquals(afl, EmvCardReader.extractAfl(tlv("80", hex("2000") + afl)))
        assertNull(EmvCardReader.extractAfl(tlv("80", hex("2000"))))
    }

    @Test
    fun aflEntries_parsesTriplesAndFallsBack() {
        assertEquals(
            listOf(Triple(1, 1, 1), Triple(2, 1, 3)),
            EmvCardReader.aflEntries(hex("08010100 10010300"))
        )
        val fallback = EmvCardReader.aflEntries(null)
        assertEquals(4, fallback.size)
        assertEquals(fallback, EmvCardReader.aflEntries(hex("080101")))          // not a multiple of 4
        assertEquals(fallback, EmvCardReader.aflEntries(hex("00010100")))        // SFI 0 is invalid
    }

    @Test
    fun extractCard_pan5aWithExpiry() {
        val rec = tlv("70", tlv("5A", hex("4111111111111111")) + tlv("5F24", hex("281231")))
        val card = EmvCardReader.extractCard(rec)
        assertNotNull(card)
        assertEquals("4111111111111111", card.cardNumber)
        assertEquals("12", card.expiryMonth)
        assertEquals("28", card.expiryYear)
    }

    @Test
    fun extractCard_pan5aIgnoresInvalidExpiryAndMissingCardData() {
        val badMonth = tlv("70", tlv("5A", hex("4111111111111111")) + tlv("5F24", hex("281331")))
        val card = EmvCardReader.extractCard(badMonth)
        assertNotNull(card)
        assertNull(card.expiryMonth)
        assertTrue(EmvCardReader.extractCard(tlv("70", tlv("5F20", "JOHN".encodeToByteArray()))) == null)
    }
}
