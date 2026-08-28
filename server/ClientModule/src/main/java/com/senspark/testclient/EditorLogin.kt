package com.senspark.testclient

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.senspark.game.handler.sol.AppendBytesObfuscate
import com.senspark.game.utils.AesEncryption
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.SecretKey

/**
 * Lấy thông tin đăng nhập từ endpoint Editor của ap-login. Client tự sinh AES key rồi gửi lên
 * (bọc RSA), ap-login chuyển tiếp cho game server, nhờ vậy test giải mã được response.
 */
class EditorLogin(
    private val apLoginBaseUrl: String = TestEnv.optional("TEST_AP_LOGIN_URL", "http://localhost:8120")!!,
    private val network: String = "bsc",
    /** Phải khớp RSA_DELIMITER của ap-login; không đặt mặc định trong mã nguồn. */
    private val rsaDelimiter: String = TestEnv.required("TEST_RSA_DELIMITER"),
) {
    private val http = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    val aesKey: SecretKey = AesEncryption.generateKey()

    data class Credentials(val jwt: String, val loginData: String)

    /** Trên production ap-login bắt username bắt đầu bằng "editor"; giữ tiền tố cho mọi môi trường. */
    fun fetch(walletAddress: String): Credentials {
        val url = "$apLoginBaseUrl/web/$network/editor_get_jwt?walletAddress=$walletAddress"
        val body = http.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            check(response.isSuccessful) { "editor_get_jwt HTTP ${response.code}: $text" }
            text
        }

        val envelope = JsonParser.parseString(body).asJsonObject
        check(envelope.get("success")?.asBoolean == true) { "editor_get_jwt failed: $body" }
        val message = envelope.getAsJsonObject("message")
        val jwt = message.get("auth").asString
        val rsaPublicKey = message.get("key").asString
        // Phải gửi lại: game server cần trường này khi giải LegacyLoginInfo.
        val extraData = message.get("extraData").asString

        return Credentials(jwt, buildLoginData(jwt, rsaPublicKey, extraData))
    }

    /** `lk` mà server chờ: RSA(JSON({aesKey, encryptedJwt})), aesKey kèm 16 byte rác ở đầu. */
    private fun buildLoginData(jwt: String, rsaPublicKeyBase64: String, extraData: String): String {
        val obfuscatedAesKey = AppendBytesObfuscate(OBFUSCATE_BYTES)
            .obfuscate(AesEncryption.exportKeyToBase64(aesKey))
        val payload = JsonObject().apply {
            addProperty("aesKey", obfuscatedAesKey)
            addProperty("encryptedJwt", jwt)
            addProperty("extraData", extraData)
        }
        return rsaEncrypt(payload.toString(), rsaPublicKeyBase64)
    }

    /** OAEP với khoá 2048-bit chỉ chứa nổi 214 byte, nên chia khối nhỏ hơn thế. */
    private fun rsaEncrypt(plain: String, publicKeyBase64: String): String {
        val keySpec = X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64))
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)

        val bytes = plain.toByteArray(Charsets.UTF_8)
        val encoder = Base64.getEncoder()
        val out = StringBuilder()
        var offset = 0
        while (offset < bytes.size) {
            val size = minOf(CHUNK_BYTES, bytes.size - offset)
            val cipher = Cipher.getInstance(RSA_OAEP_SHA1)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            out.append(encoder.encodeToString(cipher.doFinal(bytes, offset, size)))
            // Dấu phân cách đứng sau mọi khối, kể cả khối cuối.
            out.append(rsaDelimiter)
            offset += size
        }
        return out.toString()
    }

    companion object {
        private const val OBFUSCATE_BYTES = 16
        private const val CHUNK_BYTES = 190

        private const val RSA_OAEP_SHA1 = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"
    }
}
