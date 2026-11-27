package com.knightmstr.onyxtv.upload

import java.security.SecureRandom

/** The secret in the phone-setup link. 16 characters from 36 gives about 82 bits. */
object SetupToken {
    const val LENGTH = 16
    private const val ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"
    private val random = SecureRandom()

    fun generate(): String = (1..LENGTH).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")
}
