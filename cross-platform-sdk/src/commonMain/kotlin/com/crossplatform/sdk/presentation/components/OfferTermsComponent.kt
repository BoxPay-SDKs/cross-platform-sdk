package com.crossplatform.sdk.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crossplatform.sdk.presentation.theme.defaultFontFamily

fun parseHtmlParagraphs(html: String): List<String> {
    return html
        .replace("\n", " ")
        .split(Regex("<p>|</p>"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toList()
}

@Composable
fun TermsList(
    html: String,
    modifier: Modifier = Modifier
) {
    val items = remember(html) { parseHtmlParagraphs(html) }

    Column(modifier = modifier) {
        items.forEach { term ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Text(
                    text = "•",
                    modifier = Modifier.padding(end = 8.dp),
                    fontFamily = defaultFontFamily
                )
                Text(
                    text = term,
                    fontFamily = defaultFontFamily
                )
            }
        }
    }
}