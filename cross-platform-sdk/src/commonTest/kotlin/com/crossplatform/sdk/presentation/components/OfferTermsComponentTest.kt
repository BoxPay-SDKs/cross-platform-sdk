package com.crossplatform.sdk.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ParseHtmlParagraphsTest {

    @Test
    fun `splits multiple paragraph tags into separate trimmed items`() {
        val html = "<p>First term</p><p>Second term</p>"

        assertEquals(listOf("First term", "Second term"), parseHtmlParagraphs(html))
    }

    @Test
    fun `replaces newlines with spaces before splitting`() {
        val html = "<p>Line one\nLine two</p>"

        assertEquals(listOf("Line one Line two"), parseHtmlParagraphs(html))
    }

    @Test
    fun `drops empty paragraphs`() {
        val html = "<p>First</p><p></p><p>Second</p>"

        assertEquals(listOf("First", "Second"), parseHtmlParagraphs(html))
    }

    @Test
    fun `plain text with no tags is returned as a single trimmed item`() {
        assertEquals(listOf("Just plain text"), parseHtmlParagraphs("  Just plain text  "))
    }

    @Test
    fun `empty input returns an empty list`() {
        assertEquals(emptyList(), parseHtmlParagraphs(""))
    }
}
