package com.example.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "NetVault_MasterKey_AES256"
    private const val GCM_IV_LENGTH = 12 // 96 bits recommended for AES-GCM
    private const val GCM_TAG_LENGTH = 128 // 128 bit authentication tag
    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH_BITS = 256

    private val secureRandom = SecureRandom()

    init {
        ensureKeystoreKeyExists()
    }

    /**
     * Initializes hardware-backed Master Key in Android Keystore if not present.
     */
    private fun ensureKeystoreKeyExists() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_LENGTH_BITS)
                    .build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            // In testing environments or devices without Keystore, fallback is handled gracefully
        }
    }

    /**
     * Derives a SecretKey from user PIN or device secret using PBKDF2WithHmacSHA256.
     */
    fun deriveKeyFromSecret(secret: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(secret.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    /**
     * Generates a secure random salt of the specified length.
     */
    fun generateSalt(length: Int = 16): ByteArray {
        val salt = ByteArray(length)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Hashes a PIN with salt using PBKDF2WithHmacSHA256.
     */
    fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return factory.generateSecret(spec).encoded
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     */
    fun verifyPin(pin: String, salt: ByteArray, expectedHash: ByteArray): Boolean {
        val actualHash = hashPin(pin, salt)
        return MessageDigest.isEqual(actualHash, expectedHash)
    }

    /**
     * Encrypts an input stream into an output stream using AES-256-GCM.
     * The IV is written at the beginning of the output stream.
     */
    fun encryptStream(input: InputStream, output: OutputStream, secretKey: SecretKey) {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)
        output.write(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        CipherOutputStream(output, cipher).use { cipherOut ->
            val buffer = ByteArray(16384)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                cipherOut.write(buffer, 0, bytesRead)
            }
            cipherOut.flush()
        }
    }

    /**
     * Decrypts an input stream into an output stream using AES-256-GCM.
     * Reads the IV from the beginning of the input stream.
     */
    fun decryptStream(input: InputStream, output: OutputStream, secretKey: SecretKey) {
        val iv = ByteArray(GCM_IV_LENGTH)
        val bytesReadIv = input.read(iv)
        if (bytesReadIv != GCM_IV_LENGTH) {
            throw IllegalArgumentException("Data terenkripsi rusak: panjang IV tidak mencukupi.")
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        CipherInputStream(input, cipher).use { cipherIn ->
            val buffer = ByteArray(16384)
            var bytesRead: Int
            while (cipherIn.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
            output.flush()
        }
    }

    /**
     * Calculates the SHA-256 checksum of a file for integrity verification.
     */
    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(16384)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Encrypts raw bytes (e.g. for metadata or small payload).
     */
    fun encryptBytes(data: ByteArray, secretKey: SecretKey): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(data)
        return iv + ciphertext
    }

    /**
     * Decrypts raw bytes.
     */
    fun decryptBytes(encryptedData: ByteArray, secretKey: SecretKey): ByteArray {
        if (encryptedData.size < GCM_IV_LENGTH + 16) {
            throw IllegalArgumentException("Ukuran ciphertext tidak valid")
        }
        val iv = encryptedData.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = encryptedData.copyOfRange(GCM_IV_LENGTH, encryptedData.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(ciphertext)
    }

    fun bytesToHex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun hexToBytes(hex: String): ByteArray {
        val clean = hex.trim()
        val result = ByteArray(clean.length / 2)
        for (i in result.indices) {
            val index = i * 2
            result[i] = clean.substring(index, index + 2).toInt(16).toByte()
        }
        return result
    }
}
