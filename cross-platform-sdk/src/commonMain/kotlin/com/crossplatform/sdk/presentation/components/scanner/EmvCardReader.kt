package com.crossplatform.sdk.presentation.components.scanner

internal object EmvCardReader {


    fun read(transceive: (ByteArray) -> ByteArray?): ScannedCard? {
        // 1. SELECT PPSE
        val ppseResp = transceive(EmvParser.SELECT_PPSE) ?: return null
        if (!EmvParser.isSuccess(ppseResp)) return null

        val aids = EmvParser.parseAidsFromPpse(EmvParser.stripSW(ppseResp))
        if (aids.isEmpty()) return null

        // 2. SELECT AID — try each AID until one succeeds
        var selectResp: ByteArray? = null
        for (aid in aids) {
            val resp = transceive(EmvParser.buildSelectAid(aid))
            if (resp != null && EmvParser.isSuccess(resp)) { selectResp = resp; break }
        }
        if (selectResp == null) return null

        // 3. GET PROCESSING OPTIONS — answer the card's PDOL (tag 9F38) if it has one.
        //    A failed GPO is not fatal: some cards still allow READ RECORD afterwards.
        val pdol = EmvParser.findTag(EmvParser.stripSW(selectResp), 0x9F38)
        val gpoResp = transceive(EmvParser.buildGetProcessingOptions(pdol))
        val aflBytes: ByteArray? =
            if (gpoResp != null && EmvParser.isSuccess(gpoResp)) {
                extractAfl(EmvParser.stripSW(gpoResp))
            } else null

        // 4. READ RECORD
        for ((sfi, first, last) in aflEntries(aflBytes)) {
            for (rec in first..last) {
                val resp = transceive(EmvParser.buildReadRecord(sfi, rec)) ?: continue
                if (!EmvParser.isSuccess(resp)) continue
                val card = extractCard(EmvParser.stripSW(resp))
                if (card != null) return card
            }
        }
        return null
    }

    /** AFL is tag 94 in a format 2 (77) response, or the bytes after the 2-byte AIP in format 1 (80). */
    internal fun extractAfl(gpoData: ByteArray): ByteArray? =
        EmvParser.findTag(gpoData, 0x94)
            ?: EmvParser.findTag(gpoData, 0x80)?.let { fmt1 ->
                if (fmt1.size > 2) fmt1.copyOfRange(2, fmt1.size) else null
            }


    internal fun aflEntries(aflBytes: ByteArray?): List<Triple<Int, Int, Int>> {
        if (aflBytes != null && aflBytes.isNotEmpty() && aflBytes.size % 4 == 0) {
            val parsed = (aflBytes.indices step 4).mapNotNull { i ->
                val sfi   = (aflBytes[i].toInt() and 0xFF) shr 3
                val first = aflBytes[i + 1].toInt() and 0xFF
                val last  = aflBytes[i + 2].toInt() and 0xFF
                if (sfi in 1..10 && first >= 1 && last >= first) Triple(sfi, first, last) else null
            }
            if (parsed.isNotEmpty()) return parsed
        }
        return listOf(Triple(1, 1, 6), Triple(2, 1, 6), Triple(3, 1, 4), Triple(4, 1, 4))
    }

    internal fun extractCard(record: ByteArray): ScannedCard? {
        val track2 = EmvParser.findTag(record, 0x57)
        if (track2 != null) {
            val card = EmvParser.parseTrack2(track2)
            if (card?.cardNumber != null) return card
        }

        val pan5a = EmvParser.findTag(record, 0x5A)
        if (pan5a != null) {
            val pan = EmvParser.parsePanTag5A(pan5a) ?: return null
            val expBytes = EmvParser.findTag(record, 0x5F24)
            if (expBytes != null && expBytes.size >= 2) {
                // 5F24 is YYMMDD in BCD
                val hex = expBytes.joinToString("") { b ->
                    (b.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase()
                }
                val yy = hex.substring(0, 2)
                val mm = hex.substring(2, 4)
                val month = mm.toIntOrNull()
                if (month != null && month in 1..12 && yy.all { it.isDigit() }) {
                    return ScannedCard(cardNumber = pan, expiryMonth = mm, expiryYear = yy)
                }
            }
            return ScannedCard(cardNumber = pan)
        }
        return null
    }
}
