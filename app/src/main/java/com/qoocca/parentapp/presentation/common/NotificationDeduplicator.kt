package com.qoocca.parentapp.presentation.common

import java.util.concurrent.ConcurrentHashMap

object NotificationDeduplicator {
    private const val WINDOW_MS: Long = 60_000
    private val lastNotifiedAt = ConcurrentHashMap<Long, Long>()

    fun shouldSuppress(receiptId: Long): Boolean {
        val now = System.currentTimeMillis()
        prune(now)

        val previous = lastNotifiedAt[receiptId]
        if (previous != null && now - previous < WINDOW_MS) {
            return true
        }

        lastNotifiedAt[receiptId] = now
        return false
    }

    private fun prune(now: Long) {
        val iterator = lastNotifiedAt.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > WINDOW_MS) {
                iterator.remove()
            }
        }
    }
}
