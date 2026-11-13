package com.smartcodeapps.constructionprizewheel.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinPolicy {
    const val LENGTH = 4
    private val pattern = Regex("\\d{$LENGTH}")

    fun isValid(pin: String): Boolean = pattern.matches(pin)
}

object PinHasher {
    private const val ITERATIONS = 100_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    fun matches(pin: String, salt: ByteArray, expectedHash: ByteArray): Boolean =
        MessageDigest.isEqual(hash(pin, salt), expectedHash)
}
