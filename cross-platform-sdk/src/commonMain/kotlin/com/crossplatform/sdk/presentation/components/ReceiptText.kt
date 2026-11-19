package com.crossplatform.sdk.presentation.components

internal const val PRODUCT_NAME_LABEL = "Product Name"

/**
 * Splits a comma separated product name into one entry per product so each
 * can be shown on its own line. Commas stay at the end of every line except
 * the last, e.g. "Latte, Cookie" -> ["Latte,", "Cookie"].
 */
internal fun productNameLines(productName: String): List<String> {
    val names = productName.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (names.isEmpty()) return listOf(productName)
    return names.mapIndexed { index, name -> if (index < names.lastIndex) "$name," else name }
}

/**
 * Returns [text] unchanged if it fits in [maxWidth], otherwise the longest
 * prefix that fits followed by an ellipsis. [measure] returns the rendered
 * width of a string in the same unit as [maxWidth].
 */
internal fun ellipsize(text: String, maxWidth: Double, measure: (String) -> Double): String {
    if (measure(text) <= maxWidth) return text
    val ellipsis = "\u2026"
    var end = text.length
    while (end > 0 && measure(text.substring(0, end).trimEnd() + ellipsis) > maxWidth) end--
    return if (end == 0) ellipsis else text.substring(0, end).trimEnd() + ellipsis
}
