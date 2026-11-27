package com.crossplatform.sdk.data.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoRefundParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `timer api response is parsed`() {
        val body = """
            {
              "token": "11926374-4943-4452-838d-70d3a596422d",
              "transactionId": "J4ZDrDjZKM",
              "timerStarted": true,
              "autoRefundAt": "2026-11-23T10:46:12.835551837Z",
              "autoRefundTimeoutSeconds": 30
            }
        """.trimIndent()

        val parsed = json.decodeFromString<AutoRefundTimerResponse>(body)

        assertEquals("J4ZDrDjZKM", parsed.transactionId)
        assertTrue(parsed.timerStarted)
        assertEquals(30, parsed.autoRefundTimeoutSeconds)
    }

    @Test
    fun `session configs without the key means auto refund is disabled`() {
        val configs = json.decodeFromString<Configs>("""{"paymentMethods":[],"enabledFields":[]}""")

        assertFalse(configs.autoRefundEnabled)
    }

    @Test
    fun `session configs with autoRefundEnabled true enable the feature`() {
        val configs = json.decodeFromString<Configs>(
            """{"paymentMethods":[],"enabledFields":[],"autoRefundEnabled":true}"""
        )

        assertTrue(configs.autoRefundEnabled)
    }
}
