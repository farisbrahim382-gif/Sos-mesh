package com.example.mesh

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe sliding-window deduplication cache.
 * Records seen packet IDs with expiry timestamps to prevent broadcast storm relay loops.
 */
class DeduplicationCache(
    private val maxEntries: Int = 1000,
    private val expiryMillis: Long = 10 * 60 * 1000L // 10 minutes
) {
    private val seenPackets = ConcurrentHashMap<String, Long>()

    /**
     * Checks if packet has been seen. If NOT seen, marks it as seen and returns true.
     * Returns false if packet is duplicate.
     */
    @Synchronized
    fun markIfNew(packetId: String): Boolean {
        val now = System.currentTimeMillis()
        cleanExpired(now)

        if (seenPackets.containsKey(packetId)) {
            return false // Duplicate packet
        }

        if (seenPackets.size >= maxEntries) {
            val oldestKey = seenPackets.minByOrNull { it.value }?.key
            if (oldestKey != null) {
                seenPackets.remove(oldestKey)
            }
        }

        seenPackets[packetId] = now
        return true
    }

    fun isDuplicate(packetId: String): Boolean {
        val timestamp = seenPackets[packetId] ?: return false
        return (System.currentTimeMillis() - timestamp) < expiryMillis
    }

    private fun cleanExpired(now: Long) {
        val iterator = seenPackets.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > expiryMillis) {
                iterator.remove()
            }
        }
    }

    fun clear() {
        seenPackets.clear()
    }
}
