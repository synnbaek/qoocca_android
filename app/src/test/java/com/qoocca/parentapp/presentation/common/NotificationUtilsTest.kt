package com.qoocca.parentapp.presentation.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationUtilsTest {

    @Test
    fun parseReceiptId_parsesPositiveNumber() {
        val id = NotificationRouter.parseReceiptId("123")
        assertTrue(id == 123L)
    }

    @Test
    fun parseReceiptId_rejectsInvalidOrNonPositive() {
        assertNull(NotificationRouter.parseReceiptId("abc"))
        assertNull(NotificationRouter.parseReceiptId("0"))
        assertNull(NotificationRouter.parseReceiptId("-1"))
    }

    @Test
    fun deduplicator_suppressesImmediateDuplicate() {
        val first = NotificationDeduplicator.shouldSuppress(777L)
        val second = NotificationDeduplicator.shouldSuppress(777L)

        assertFalse(first)
        assertTrue(second)
    }
}
