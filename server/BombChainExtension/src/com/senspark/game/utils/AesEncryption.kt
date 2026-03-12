package com.senspark.game.utils

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object AesEncryption {
    private const val AES = "AES"
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_SIZE = 256 // Use 256 bits for AES (requires JCE Unlimited Strength Policy for older JVMs)
    private const val IV_SIZE = 16 // 16 bytes for AES block size

    // Generate a new AES key
    fun generateKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance(AES)
        keyGen.init(KEY_SIZE)
        return keyGen.generateKey()
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun encrypt(plainText: String, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encode(iv + cipherBytes)
    }
    
    fun encryptToBytes(plainText: String, secretKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        val ivSpec = IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return iv + cipherBytes
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decrypt(base64CipherText: String, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val bytes = Base64.decode(base64CipherText)
        val iv = bytes.copyOf(IV_SIZE)
        val ivSpec = IvParameterSpec(iv)
        val cipherBytes = bytes.copyOfRange(IV_SIZE, bytes.size)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        return cipher.doFinal(cipherBytes).decodeToString()
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun exportKeyToBase64(secretKey: SecretKey): String {
        return Base64.encode(secretKey.encoded)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun importKeyFromBase64(base64Key: String): SecretKey {
        val decodedKey = Base64.decode(base64Key)
        return SecretKeySpec(decodedKey, AES)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun exportIvToBase64(): String {
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        return Base64.encode(iv)
    }
}