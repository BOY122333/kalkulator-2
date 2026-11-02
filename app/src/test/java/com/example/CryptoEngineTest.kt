package com.example

import com.example.security.CryptoEngine
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class CryptoEngineTest {

    @Test
    fun testKeyDerivationAndPinVerification() {
        val pin = "123456"
        val salt = CryptoEngine.generateSalt(16)
        val hash = CryptoEngine.hashPin(pin, salt)

        // Correct PIN
        val isMatch = CryptoEngine.verifyPin("123456", salt, hash)
        assertTrue(isMatch)

        // Wrong PIN
        val isWrong = CryptoEngine.verifyPin("654321", salt, hash)
        assertFalse(isWrong)
    }

    @Test
    fun testStreamEncryptionDecryption() {
        val secret = "test-secret-vault-key-2026"
        val salt = CryptoEngine.generateSalt(16)
        val key = CryptoEngine.deriveKeyFromSecret(secret, salt)

        val originalText = "Informasi rahasia jaringan NetVault: 192.168.1.1/24"
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)

        val encryptedOut = ByteArrayOutputStream()
        CryptoEngine.encryptStream(ByteArrayInputStream(originalBytes), encryptedOut, key)

        val cipherBytes = encryptedOut.toByteArray()
        assertTrue(cipherBytes.size > originalBytes.size)

        val decryptedOut = ByteArrayOutputStream()
        CryptoEngine.decryptStream(ByteArrayInputStream(cipherBytes), decryptedOut, key)

        val decryptedText = decryptedOut.toString(Charsets.UTF_8.name())
        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testSha256Checksum() {
        val tempFile = File.createTempFile("test_sha", ".txt")
        try {
            tempFile.writeText("NetVault Test Payload 12345")
            val sha = CryptoEngine.calculateSha256(tempFile)
            assertEquals(64, sha.length)
        } finally {
            tempFile.delete()
        }
    }
}
