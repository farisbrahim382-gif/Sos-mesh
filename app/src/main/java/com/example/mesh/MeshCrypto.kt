package com.example.mesh

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

object MeshCrypto {
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private val secureRandom = SecureRandom()

    // Default shared channel key derived deterministically for off-grid public emergency coordination
    private val DEFAULT_MESH_SECRET = "SOS_MESH_OFFGRID_SECURE_KEY_2026".toByteArray()

    fun deriveKey(channelOrPassword: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(DEFAULT_MESH_SECRET)
        val keyBytes = digest.digest(channelOrPassword.toByteArray())
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts plaintext using AES-GCM (Authenticated Encryption).
     * Format: [12 bytes IV] + [Ciphertext + 16 bytes GCM Auth Tag]
     */
    fun encrypt(plaintext: ByteArray, key: SecretKeySpec): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)

        val ciphertext = cipher.doFinal(plaintext)
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return combined
    }

    /**
     * Decrypts ciphertext using AES-GCM. Returns null if auth tag fails (tampered or wrong key).
     */
    fun decrypt(encryptedPayload: ByteArray, key: SecretKeySpec): ByteArray? {
        if (encryptedPayload.size < GCM_IV_LENGTH + 16) return null
        return try {
            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(encryptedPayload, 0, iv, 0, GCM_IV_LENGTH)

            val ciphertextLength = encryptedPayload.size - GCM_IV_LENGTH
            val ciphertext = ByteArray(ciphertextLength)
            System.arraycopy(encryptedPayload, GCM_IV_LENGTH, ciphertext, 0, ciphertextLength)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            cipher.doFinal(ciphertext)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Computes HMAC-SHA256 signature for packet authentication
     */
    fun calculateHmac(data: ByteArray, secret: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        return mac.doFinal(data)
    }
}
