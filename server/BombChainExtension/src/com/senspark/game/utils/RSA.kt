package com.senspark.game.utils

import com.senspark.game.pvp.utility.LongBitDecoder
import java.security.PrivateKey
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher

data class RsaKeys(val publicKey: String, val privateKey: PrivateKey, val privateKeyStr: String)
data class ExplodeData(val heroId: Int, val bombNo: Int, val colBoom: Int, val rowBoom: Int)
object RSA {
    private const val ALGORITHM = "RSA"
    private const val KEY_SIZE = 1024
    
    private val _secureRandom = SecureRandom()
    private val _base64Encoder = Base64.getEncoder()
    private val _base64Decoder = Base64.getDecoder()

    /**
     * Maximum chỉ support 64 bit
     */
    fun deobfuscateDataExplodeHandler(data: Long): ExplodeData {
        val bitDecoder = LongBitDecoder(data)
        val heroId = bitDecoder.popInt(32)  // 32 bit [0, 4_294_967_295]
        val bombNo = bitDecoder.popInt(5)   // 5 bit [0, 31]
        val colBoom = bitDecoder.popInt(8)  // 8 bit [0, 255]
        val rowBoom = bitDecoder.popInt(8)  // 8 bit [0, 255]
        // ignore 5 bit cuối
        return ExplodeData(heroId, bombNo, colBoom, rowBoom)
    }

    /**
     * Tạo ra 1 cặp public key & private key
     * public key: giao cho client để mã hóa data
     * private key: server giữ để giải mã data
     * @return publicKey, privateKey
     */
    fun generateRSAKeys(): RsaKeys {
        val rsa = java.security.KeyPairGenerator.getInstance(ALGORITHM)
        rsa.initialize(KEY_SIZE, _secureRandom)

        val kp = rsa.genKeyPair()
        val publicKeyStr = _base64Encoder.encodeToString(kp.public.encoded)
        val privateKeyStr = _base64Encoder.encodeToString(kp.private.encoded)
        return RsaKeys(publicKeyStr, kp.private, privateKeyStr)
    }

    /**
     * Mã hoá dùng RSA (public key)
     * @param[plainText] utf string
     * @param[publicKey] Base64 String
     * @return Base64 String
     */
    fun rsaEncryption(plainText: String, publicKey: String): String {
        val bytes = _base64Decoder.decode(publicKey)
        val spec = java.security.spec.X509EncodedKeySpec(bytes)
        val pk = java.security.KeyFactory.getInstance(ALGORITHM).generatePublic(spec)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, pk)
        val encrypted = cipher.doFinal(plainText.toByteArray())
        return _base64Encoder.encodeToString(encrypted)
    }

    /**
     * Giải mã dùng RSA (private key)
     * @param[cipherText] Base64 String
     * @param[privateKey] Base64 String
     * @return utf string
     */
    fun rsaDecryption(cipherText: String, privateKey: String): String {
        val bytes = _base64Decoder.decode(privateKey)
        val spec = java.security.spec.PKCS8EncodedKeySpec(bytes)
        val pk = java.security.KeyFactory.getInstance(ALGORITHM).generatePrivate(spec)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, pk)
        val decrypted = cipher.doFinal(_base64Decoder.decode(cipherText))
        return String(decrypted)
    }

    /**
     * Giải mã dùng RSA (private key)
     * @param[cipherText] Base64 String
     * @param[privateKey] Object
     * @return utf string
     */
    fun rsaDecryption(cipherText: String, privateKey: PrivateKey): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val decrypted = cipher.doFinal(_base64Decoder.decode(cipherText))
        return String(decrypted)
    }
}