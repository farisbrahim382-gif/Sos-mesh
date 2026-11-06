package com.example

import com.example.mesh.DeduplicationCache
import com.example.mesh.MeshCrypto
import com.example.mesh.MeshPacket
import com.example.mesh.PacketPriority
import com.example.mesh.PacketType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class MeshPacketTest {

    @Test
    fun testMeshPacketSerializationAndDeserialization() {
        val payloadStr = "Emergency alert: Medical aid needed at Sector 4"
        val originalPacket = MeshPacket(
            packetId = "TEST-001",
            packetType = PacketType.SOS_ALERT,
            priority = PacketPriority.CRITICAL_SOS,
            hopLimit = 7,
            hopsTraveled = 2,
            senderId = "NODE-A1",
            recipientId = "BROADCAST",
            channel = "#all-mesh-broadcast",
            payload = payloadStr.toByteArray(StandardCharsets.UTF_8)
        )

        val bytes = originalPacket.toByteArray()
        assertNotNull(bytes)
        assertTrue(bytes.size > 40)

        val deserialized = MeshPacket.fromByteArray(bytes)
        assertNotNull(deserialized)
        assertEquals(originalPacket.packetId, deserialized!!.packetId)
        assertEquals(originalPacket.packetType, deserialized.packetType)
        assertEquals(originalPacket.priority, deserialized.priority)
        assertEquals(originalPacket.hopLimit, deserialized.hopLimit)
        assertEquals(originalPacket.hopsTraveled, deserialized.hopsTraveled)
        assertEquals(originalPacket.senderId, deserialized.senderId)
        assertEquals(payloadStr, String(deserialized.payload, StandardCharsets.UTF_8))
    }

    @Test
    fun testAesGcmEncryptionDecryption() {
        val secretKey = MeshCrypto.deriveKey("#medical-response")
        val secretMessage = "Confidential patient vitals: BP 120/80 HR 72"
        val plaintextBytes = secretMessage.toByteArray(StandardCharsets.UTF_8)

        val encrypted = MeshCrypto.encrypt(plaintextBytes, secretKey)
        assertNotNull(encrypted)
        assertTrue(encrypted.size > plaintextBytes.size)

        val decrypted = MeshCrypto.decrypt(encrypted, secretKey)
        assertNotNull(decrypted)
        assertEquals(secretMessage, String(decrypted!!, StandardCharsets.UTF_8))
    }

    @Test
    fun testDeduplicationCachePreventsRelayLoops() {
        val cache = DeduplicationCache(maxEntries = 50, expiryMillis = 10_000L)
        val packetId = "PKT-UNIQUE-99"

        // First arrival is new
        assertTrue(cache.markIfNew(packetId))

        // Subsequent arrival within window is recognized as duplicate and dropped
        assertFalse(cache.markIfNew(packetId))
        assertTrue(cache.isDuplicate(packetId))

        // Different packet is accepted
        assertTrue(cache.markIfNew("PKT-OTHER-100"))
    }
}
