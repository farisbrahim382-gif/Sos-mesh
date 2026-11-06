package com.example.mesh

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.UUID

enum class PacketType(val code: Byte) {
    SOS_ALERT(0x01),
    CHAT_MESSAGE(0x02),
    NODE_HEARTBEAT(0x03),
    ACK_RECEIPT(0x04),
    LOCATION_TELEMETRY(0x05);

    companion object {
        fun fromCode(code: Byte): PacketType = entries.find { it.code == code } ?: CHAT_MESSAGE
    }
}

enum class PacketPriority(val code: Byte) {
    NORMAL(0x00),
    HIGH(0x01),
    CRITICAL_SOS(0x02);

    companion object {
        fun fromCode(code: Byte): PacketPriority = entries.find { it.code == code } ?: NORMAL
    }
}

data class MeshPacket(
    val packetId: String = UUID.randomUUID().toString().substring(0, 8),
    val packetType: PacketType,
    val priority: PacketPriority,
    val hopLimit: Int = 7,
    val hopsTraveled: Int = 0,
    val senderId: String, // e.g. "NODE-001"
    val recipientId: String = "BROADCAST",
    val channel: String = "#all-mesh-broadcast",
    val payload: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MeshPacket
        return packetId == other.packetId
    }

    override fun hashCode(): Int = packetId.hashCode()

    /**
     * Serializes this packet into a binary frame suitable for BLE/LoRa transmission.
     * Frame format:
     * [0..1] MAGIC: 0x53, 0x4D ('S', 'M')
     * [2] VERSION: 0x01
     * [3] TYPE: 1 byte
     * [4] PRIORITY: 1 byte
     * [5] HOP_LIMIT: 1 byte
     * [6] HOPS_TRAVELED: 1 byte
     * [7..14] PACKET_ID: 8 bytes ASCII
     * [15..22] SENDER_ID: 8 bytes ASCII
     * [23..30] RECIPIENT_ID: 8 bytes ASCII
     * [31..38] TIMESTAMP: 8 bytes Long
     * [39..40] PAYLOAD_LEN: 2 bytes Short
     * [41..41+LEN-1] PAYLOAD: N bytes
     * [..] CRC16: 2 bytes Short
     */
    fun toByteArray(): ByteArray {
        val payloadLen = payload.size.coerceAtMost(512)
        val totalSize = 2 + 1 + 1 + 1 + 1 + 1 + 8 + 8 + 8 + 8 + 2 + payloadLen + 2
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN)

        buffer.put(0x53.toByte()) // 'S'
        buffer.put(0x4D.toByte()) // 'M'
        buffer.put(0x01.toByte()) // Version 1
        buffer.put(packetType.code)
        buffer.put(priority.code)
        buffer.put(hopLimit.toByte())
        buffer.put(hopsTraveled.toByte())

        buffer.put(padStringToBytes(packetId, 8))
        buffer.put(padStringToBytes(senderId, 8))
        buffer.put(padStringToBytes(recipientId, 8))
        buffer.putLong(timestamp)
        buffer.putShort(payloadLen.toShort())
        buffer.put(payload, 0, payloadLen)

        // Calculate CRC16 over all header and payload bytes
        val dataBeforeCrc = buffer.array().copyOfRange(0, totalSize - 2)
        val crc = calculateCrc16(dataBeforeCrc)
        buffer.putShort(crc.toShort())

        return buffer.array()
    }

    companion object {
        private const val MAGIC_0 = 0x53.toByte()
        private const val MAGIC_1 = 0x4D.toByte()

        fun fromByteArray(bytes: ByteArray): MeshPacket? {
            if (bytes.size < 43) return null // Minimum header size + CRC
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

            val m0 = buffer.get()
            val m1 = buffer.get()
            if (m0 != MAGIC_0 || m1 != MAGIC_1) return null

            val version = buffer.get()
            if (version != 0x01.toByte()) return null

            val typeCode = buffer.get()
            val priorityCode = buffer.get()
            val hopLimit = buffer.get().toInt()
            val hopsTraveled = buffer.get().toInt()

            val packetIdBytes = ByteArray(8)
            buffer.get(packetIdBytes)
            val packetId = String(packetIdBytes, StandardCharsets.UTF_8).trim()

            val senderIdBytes = ByteArray(8)
            buffer.get(senderIdBytes)
            val senderId = String(senderIdBytes, StandardCharsets.UTF_8).trim()

            val recipientIdBytes = ByteArray(8)
            buffer.get(recipientIdBytes)
            val recipientId = String(recipientIdBytes, StandardCharsets.UTF_8).trim()

            val timestamp = buffer.long
            val payloadLen = buffer.short.toInt() and 0xFFFF

            if (buffer.remaining() < payloadLen + 2) return null

            val payload = ByteArray(payloadLen)
            buffer.get(payload)

            val expectedCrc = buffer.short.toInt() and 0xFFFF
            val actualCrc = calculateCrc16(bytes.copyOfRange(0, bytes.size - 2))

            if (expectedCrc != actualCrc) return null // Frame corrupted

            return MeshPacket(
                packetId = packetId,
                packetType = PacketType.fromCode(typeCode),
                priority = PacketPriority.fromCode(priorityCode),
                hopLimit = hopLimit,
                hopsTraveled = hopsTraveled,
                senderId = senderId,
                recipientId = recipientId,
                payload = payload,
                timestamp = timestamp
            )
        }

        private fun padStringToBytes(str: String, length: Int): ByteArray {
            val result = ByteArray(length)
            val strBytes = str.toByteArray(StandardCharsets.UTF_8)
            for (i in 0 until length) {
                result[i] = if (i < strBytes.size) strBytes[i] else ' '.code.toByte()
            }
            return result
        }

        fun calculateCrc16(data: ByteArray): Int {
            var crc = 0xFFFF
            for (b in data) {
                crc = crc xor ((b.toInt() and 0xFF) shl 8)
                for (i in 0 until 8) {
                    crc = if ((crc and 0x8000) != 0) {
                        ((crc shl 1) xor 0x1021) and 0xFFFF
                    } else {
                        (crc shl 1) and 0xFFFF
                    }
                }
            }
            return crc
        }
    }
}
