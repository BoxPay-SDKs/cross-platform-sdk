package com.crossplatform.sdk.presentation.components.scanner

internal object EmvParser {

    val SELECT_PPSE: ByteArray = byteArrayOf(
        0x00.toByte(), 0xA4.toByte(), 0x04.toByte(), 0x00.toByte(),
        0x0E.toByte(),
        // "2PAY.SYS.DDF01"
        0x32, 0x50, 0x41, 0x59, 0x2E, 0x53, 0x59, 0x53, 0x2E,
        0x44, 0x44, 0x46, 0x30, 0x31,
        0x00.toByte()
    )

    fun buildSelectAid(aid: ByteArray): ByteArray =
        byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte()) + aid + byteArrayOf(0x00)


    fun buildGetProcessingOptions(pdol: ByteArray?): ByteArray {
        val data = if (pdol == null || pdol.isEmpty()) ByteArray(0) else buildPdolData(pdol)
        // 83 <len> <data> — short-form length only (data is capped well below 128 bytes)
        val body = if (data.size > MAX_PDOL_DATA) {
            byteArrayOf(0x83.toByte(), 0x00)
        } else {
            byteArrayOf(0x83.toByte(), data.size.toByte()) + data
        }
        return byteArrayOf(0x80.toByte(), 0xA8.toByte(), 0x00, 0x00, body.size.toByte()) +
                body + byteArrayOf(0x00)
    }

    private const val MAX_PDOL_DATA = 120


    fun parseDol(dol: ByteArray): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        var i = 0
        while (i < dol.size) {
            // --- tag (1..3 bytes) ---
            var tag = dol[i].toInt() and 0xFF
            val multiByte = (tag and 0x1F) == 0x1F
            i++
            if (multiByte) {
                while (i < dol.size) {
                    val b = dol[i].toInt() and 0xFF
                    tag = (tag shl 8) or b
                    i++
                    if ((b and 0x80) == 0) break   // last tag byte has bit 8 clear
                }
            }
            // --- length (single byte in a DOL) ---
            if (i >= dol.size) break
            val len = dol[i].toInt() and 0xFF
            i++
            result.add(tag to len)
        }
        return result
    }


    fun buildPdolData(pdol: ByteArray): ByteArray {
        val out = ArrayList<Byte>()
        for ((tag, len) in parseDol(pdol)) {
            val value = when (tag) {
                0x9F66 -> byteArrayOf(0x27, 0x00, 0x00, 0x00)              // Terminal Transaction Qualifiers
                0x9F1A, 0x5F2A -> byteArrayOf(0x03, 0x56)                  // Terminal country / transaction currency (INR)
                0x9C   -> byteArrayOf(0x00)                                 // Transaction type: purchase
                0x9F35 -> byteArrayOf(0x22)                                 // Terminal type: attended, offline with online capability
                0x9F37 -> kotlin.random.Random.nextBytes(4)                 // Unpredictable number
                else   -> ByteArray(0)                                      // amounts, TVR, date… → zeros
            }
            out.addAll(fitToLength(value, len).toList())
        }
        return out.toByteArray()
    }

    private fun fitToLength(value: ByteArray, len: Int): ByteArray = when {
        value.size == len -> value
        value.size > len  -> value.copyOfRange(value.size - len, value.size)
        else              -> ByteArray(len - value.size) + value
    }


    fun buildReadRecord(sfi: Int, record: Int): ByteArray =
        byteArrayOf(0x00, 0xB2.toByte(), record.toByte(), ((sfi shl 3) or 4).toByte(), 0x00)

    fun isSuccess(response: ByteArray): Boolean =
        response.size >= 2 &&
                response[response.size - 2] == 0x90.toByte() &&
                response[response.size - 1] == 0x00.toByte()

    fun stripSW(response: ByteArray): ByteArray =
        if (response.size >= 2) response.copyOf(response.size - 2) else response

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
