package com.crossplatform.sdk.presentation.components.scanner

/**
 * Result of a scan. Every field is optional because OCR may only read part of the card.
 * cardNumber is digits only; expiryMonth is "01".."12"; expiryYear is two digits.
 */
internal data class ScannedCard(
    val cardNumber: String? = null,
    val expiryMonth: String? = null,
    val expiryYear: String? = null,
    val holderName: String? = null
)

internal enum class CardScanError { PermissionDenied, CameraUnavailable }

/**
 * Turns the raw text lines an OCR engine returns for ONE camera frame into card fields.
 * Pure Kotlin so Android and iOS share exactly the same behaviour.
 */
internal object CardTextParser {

    private val numberRun = Regex("""\d(?:[ \-]?\d){11,18}""")
    private val numericOnlyLine = Regex("""^[\d \-]+$""")
    private val expiryRegex = Regex("""\b(0[1-9]|1[0-2])\s?[/\-.]\s?(\d{4}|\d{2})\b""")
    private val nameShape = Regex("""^[A-Za-z][A-Za-z .'\-]{4,28}$""")

    private val nameStopWords = setOf(
        "VALID", "THRU", "THROUGH", "FROM", "GOOD", "MONTH", "YEAR", "DEBIT", "CREDIT", "CARD",
        "BANK", "BANKING", "VISA", "MASTERCARD", "MASTER", "RUPAY", "AMEX", "AMERICAN", "EXPRESS",
        "DISCOVER", "DINERS", "CLUB", "PLATINUM", "GOLD", "SILVER", "CLASSIC", "SIGNATURE",
        "INFINITE", "WORLD", "TITANIUM", "BUSINESS", "CORPORATE", "PREPAID", "CONTACTLESS",
        "ELECTRON", "MAESTRO", "INTERNATIONAL", "MEMBER", "SINCE", "EXPIRES", "EXPIRY", "END",
        "CVV", "CVC", "AUTHORIZED", "AUTHORISED", "PAYMENT", "LIMITED", "LTD", "FINANCE", "INDIA",
        "REWARDS", "SELECT", "PREMIUM", "PAY", "TAP", "ISSUED", "CUSTOMER", "CARE", "SERVICE"
    )

    fun parse(rawLines: List<String>): ScannedCard {
        val lines = rawLines.map { it.trim() }.filter { it.isNotEmpty() }
        val (number, numberLine) = findNumber(lines)
        val (month, year) = findExpiry(lines)
        // Only trust a name that appears below the number, so we never pick up the bank name.
        val name = if (numberLine >= 0) findName(lines, numberLine) else null
        return ScannedCard(number, month, year, name)
    }

    // ---------------------------------------------------------------- number

    private fun findNumber(lines: List<String>): Pair<String?, Int> {
        for (i in lines.indices) {
            extractNumber(fixOcrDigits(lines[i]))?.let { return it to i }
        }

        // Some cards print the number as several short numeric-only lines (grouped 4, 8, or any
        // other split per line) instead of one continuous line. Gather just those lines, in
        // order, and try joining runs of them — skipping over any non-numeric line that lands in
        // between (a bank caption, a stray misread character, part of a logo) rather than
        // bailing the moment one shows up. A run only breaks when two numeric lines are too far
        // apart in the frame to plausibly be the same printed number.
        val numericIndices = lines.indices.filter { numericOnlyLine.matches(lines[it]) }
        for (start in numericIndices.indices) {
            var joined = lines[numericIndices[start]]
            extractNumber(joined)?.let { return it to numericIndices[start] }
            for (end in (start + 1) until numericIndices.size) {
                if (numericIndices[end] - numericIndices[end - 1] > 3) break
                joined += " " + lines[numericIndices[end]]
                extractNumber(joined)?.let { return it to numericIndices[end] }
            }
        }
        return null to -1
    }

    private fun extractNumber(text: String): String? {
        for (m in numberRun.findAll(text)) {
            val digits = m.value.filter { it.isDigit() }
            val first = digits[0]
            if (first !in '2'..'6' && first != '8') continue // Amex/Visa/MC/Discover/RuPay/UnionPay

            if (digits.length in 13..19 && isLuhnValid(digits)) return digits

            // The expiry digits sometimes glue onto the number ("...1111 1226").
            if (digits.length > 16) {
                for (len in intArrayOf(16, 15)) {
                    val candidate = digits.take(len)
                    if (digits.length - len <= 4 && isLuhnValid(candidate)) return candidate
                }
            }
        }
        return null
    }

    /** In lines that are clearly a card number, fix the usual OCR letter/digit mix-ups. */
    private fun fixOcrDigits(line: String): String {
        if (line.count { it.isDigit() } < 10) return line
        return line.map {
            when (it) {
                'O', 'o', 'Q', 'D' -> '0'
                'I', 'l', '|' -> '1'
                'S' -> '5'
                'B' -> '8'
                else -> it
            }
        }.joinToString("")
    }

    internal fun isLuhnValid(digits: String): Boolean {
        var sum = 0
        var doubleIt = false
        for (i in digits.length - 1 downTo 0) {
            var n = digits[i] - '0'
            if (doubleIt) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            doubleIt = !doubleIt
        }
        return sum % 10 == 0
    }

    // ---------------------------------------------------------------- expiry

    private fun findExpiry(lines: List<String>): Pair<String?, String?> {
        var bestYear = -1
        var bestMonth = -1
        for (line in lines) {
            if (line.count { it.isDigit() } >= 12) continue // that's the card number line
            for (m in expiryRegex.findAll(line)) {
                val start = m.range.first
                // Scan the whole line prefix, not a fixed-width window: a short window can
                // truncate a keyword right at its start (e.g. "MEMBER SINCE " is 13 chars,
                // one more than a 12-char lookback, which used to clip the leading "m").
                val before = line.substring(0, start).lowercase()
                if ("from" in before || "since" in before || "member" in before) continue

                val month = m.groupValues[1].toInt()
                val year = m.groupValues[2].takeLast(2).toInt()
                // Cards may also print "valid from"; the expiry is the latest date on the card.
                if (year > bestYear || (year == bestYear && month > bestMonth)) {
                    bestYear = year
                    bestMonth = month
                }
            }
        }
        if (bestYear < 0) return null to null
        return bestMonth.toString().padStart(2, '0') to bestYear.toString().padStart(2, '0')
    }

    // ---------------------------------------------------------------- name

    private fun findName(lines: List<String>, numberLine: Int): String? {
        for (i in numberLine + 1 until lines.size) {
            val text = lines[i].trim()
            if (!nameShape.matches(text)) continue
            val words = text.uppercase().split(' ', '.').filter { it.isNotEmpty() }
            if (words.size < 2) continue
            if (words.any { it in nameStopWords }) continue
            return words.joinToString(" ")
        }
        return null
    }
}

/**
 * OCR is noisy frame to frame, so don't trust the first read. A number is accepted once the
 * same Luhn-valid number has been seen in [requiredNumberHits] frames; then we wait a few more
 * frames for the expiry (which is often on the same frame, so this usually ends immediately).
 */
internal class CardScanAggregator(
    private val requiredNumberHits: Int = 2,
    private val extraFramesForExpiry: Int = 6
) {
    private val hits = mutableMapOf<String, Int>()
    private var confirmed: String? = null
    private var extraFrames = 0
    private var month: String? = null
    private var year: String? = null
    private var name: String? = null
    private var finished = false

    fun accept(frame: ScannedCard): ScannedCard? {
        if (finished) return null

        if (frame.expiryMonth != null && frame.expiryYear != null) {
            month = frame.expiryMonth
            year = frame.expiryYear
        }
        if (frame.holderName != null) name = frame.holderName

        val n = frame.cardNumber
        if (confirmed == null) {
            if (n != null) {
                val count = (hits[n] ?: 0) + 1
                hits[n] = count
                if (count >= requiredNumberHits) confirmed = n
            }
        } else {
            extraFrames++
        }

        val number = confirmed ?: return null
        if (month != null || extraFrames >= extraFramesForExpiry) {
            finished = true
            return ScannedCard(number, month, year, name)
        }
        return null
    }
}