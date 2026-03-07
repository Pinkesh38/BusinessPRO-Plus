package com.example.businessproplus

import android.util.Base64
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityUtils {
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    fun hashPin(pin: String): Pair<String, String> {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        
        val saltString = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashString = performHash(pin, salt)
        
        return Pair(hashString, saltString)
    }

    fun verifyPin(pin: String, storedHash: String, saltString: String): Boolean {
        val salt = Base64.decode(saltString, Base64.NO_WRAP)
        val hashAttempt = performHash(pin, salt)
        return hashAttempt == storedHash
    }

    private fun performHash(pin: String, salt: ByteArray): String {
        val spec: KeySpec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
}