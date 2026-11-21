package com.knightmstr.firelauncher.data

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Salted SHA-256 hashing for the Kids mode PIN, so the PIN itself is never stored.
 * Stored form: "<saltHex>:<hashHex>".
 */
object PinHasher {

    const val PIN_LENGTH = 4

    fun isValidPin(pin: String): Boolean = pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }

    fun hash(pin: String, salt: ByteArray = newSalt()): String =
        "${salt.toHex()}:${digest(salt, pin).toHex()}"

    fun verify(pin: String, stored: String?): Boolean {
        if (stored.isNullOrBlank() || !isValidPin(pin)) return false
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = parts[0].hexToBytes() ?: return false
        val expected = parts[1].hexToBytes() ?: return false
        return MessageDigest.isEqual(expected, digest(salt, pin))
    }

    private fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    private fun digest(salt: ByteArray, pin: String): ByteArray =
        MessageDigest.getInstance("SHA-256").run {
            update(salt)
            digest(pin.toByteArray(Charsets.UTF_8))
        }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray? {
        if (length % 2 != 0) return null
        return runCatching { chunked(2).map { it.toInt(16).toByte() }.toByteArray() }.getOrNull()
    }
}
