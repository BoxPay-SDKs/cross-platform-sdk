package com.crossplatform.sdk.presentation

import com.crossplatform.sdk.domain.model.TransactionStatusEnum
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [getStatus] maps the payment (initiate) response status and
 * [getFetchStatus] maps the fetch-status response status. POSTED and
 * PROCESSING both map to [TransactionStatusEnum.PROCESSING], which the UI
 * treats like SUCCESS (the Payment Successful page is shown).
 */
class TransactionStatusMappingTest {

    @Test
    fun `getStatus maps POSTED and PROCESSING to PROCESSING`() {
        assertEquals(TransactionStatusEnum.PROCESSING, getStatus("POSTED"))
        assertEquals(TransactionStatusEnum.PROCESSING, getStatus("PROCESSING"))
    }

    @Test
    fun `getStatus is case insensitive for PROCESSING statuses`() {
        assertEquals(TransactionStatusEnum.PROCESSING, getStatus("posted"))
        assertEquals(TransactionStatusEnum.PROCESSING, getStatus("Processing"))
    }

    @Test
    fun `getStatus keeps the existing mappings`() {
        listOf("APPROVED", "SUCCESS", "PAID").forEach {
            assertEquals(TransactionStatusEnum.SUCCESS, getStatus(it), it)
        }
        listOf("FAILED", "REJECTED").forEach {
            assertEquals(TransactionStatusEnum.FAILED, getStatus(it), it)
        }
        assertEquals(TransactionStatusEnum.EXPIRED, getStatus("EXPIRED"))
        assertEquals(TransactionStatusEnum.REQUIRESACTION, getStatus("REQUIRESACTION"))
        assertEquals(TransactionStatusEnum.NOACTION, getStatus("SOMETHING_ELSE"))
        assertEquals(TransactionStatusEnum.NOACTION, getStatus(""))
    }

    @Test
    fun `getFetchStatus maps POSTED and PROCESSING to PROCESSING`() {
        assertEquals(TransactionStatusEnum.PROCESSING, getFetchStatus("POSTED"))
        assertEquals(TransactionStatusEnum.PROCESSING, getFetchStatus("PROCESSING"))
        assertEquals(TransactionStatusEnum.PROCESSING, getFetchStatus("posted"))
    }

    @Test
    fun `getFetchStatus keeps the existing mappings and falls back to REQUIRESACTION`() {
        listOf("APPROVED", "SUCCESS", "PAID").forEach {
            assertEquals(TransactionStatusEnum.SUCCESS, getFetchStatus(it), it)
        }
        listOf("FAILED", "REJECTED").forEach {
            assertEquals(TransactionStatusEnum.FAILED, getFetchStatus(it), it)
        }
        assertEquals(TransactionStatusEnum.EXPIRED, getFetchStatus("EXPIRED"))
        assertEquals(TransactionStatusEnum.REQUIRESACTION, getFetchStatus("PENDING"))
    }
}
