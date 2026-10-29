package app.opora.core

import app.opora.core.model.PinData
import java.security.MessageDigest
import java.security.SecureRandom

/** PIN хранится только как SHA-256 от соли и PIN. */
object PinHash {
    private val DIGITS = Regex("\\d{4,8}")

    fun isValid(pin: String): Boolean = DIGITS.matches(pin)

    fun create(pin: String, random: SecureRandom = SecureRandom()): PinData {
        val salt = ByteArray(16).also(random::nextBytes).toHex()
        return PinData(salt = salt, hash = hash(salt, pin))
    }

    fun verify(pin: String, data: PinData): Boolean =
        MessageDigest.isEqual(hash(data.salt, pin).toByteArray(), data.hash.toByteArray())

    private fun hash(salt: String, pin: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
