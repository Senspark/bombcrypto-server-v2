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
 * Lấy thông tin đăng nhập từ endpoint Editor của ap-login và dựng gói login mà server chờ đợi.
 *
 * Cặp khoá: client tự sinh AES key rồi gửi lên (bọc RSA), ap-login gỡ ra và chuyển cho game
 * server thành `user.aesKey`. Nhờ vậy test biết khoá ngay từ đầu và giải mã được response —
 * chứ không phải xin khoá về.
 */
class EditorLogin(
    private val apLoginBaseUrl: String = TestEnv.optional("TEST_AP_LOGIN_URL", "http://localhost:8120")!!,
    private val network: String = "bsc",
    /**
     * Phải khớp RSA_DELIMITER của ap-login. Đây là cấu hình bí mật của server nên không có giá
     * trị mặc định trong mã nguồn — lấy từ môi trường, thiếu thì dừng hẳn.
     */
    private val rsaDelimiter: String = TestEnv.required("TEST_RSA_DELIMITER"),
) {
    private val http = OkHttpClient.Builder()
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    /** Khoá AES của phiên này; chính là khoá game server dùng để mã hoá response. */
    val aesKey: SecretKey = AesEncryption.generateKey()

    data class Credentials(val jwt: String, val loginData: String)

    /**
     * [walletAddress] gấp đôi làm username. Trên production, ap-login bắt nó phải bắt đầu bằng
     * "editor"; local thì không, nhưng cứ giữ tiền tố để test chạy được ở mọi môi trường.
     */
    fun fetch(walletAddress: String): Credentials {
        val url = "$apLoginBaseUrl/web/$network/editor_get_jwt?walletAddress=$walletAddress"
        val body = http.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            check(response.isSuccessful) { "editor_get_jwt HTTP ${response.code}: $text" }
            text
        }

        val envelope = JsonParser.parseString(body).asJsonObject
        check(envelope.get("success")?.asBoolean == true) { "editor_get_jwt failed: $body" }
        // ap-login đặt payload vào "message", không phải "data".
        val message = envelope.getAsJsonObject("message")
        val jwt = message.get("auth").asString
        val rsaPublicKey = message.get("key").asString
        // Phải gửi extraData trở lại: /web/bsc/verify phản chiếu nguyên xi trường này từ loginData,
        // và game server bắt buộc phải có nó khi giải LegacyLoginInfo. Thiếu là login hỏng.
        val extraData = message.get("extraData").asString

        return Credentials(jwt, buildLoginData(jwt, rsaPublicKey, extraData))
    }

    /**
     * `lk` mà server chờ: RSA(JSON({aesKey, encryptedJwt})).
     * aesKey đi kèm 16 byte rác ở đầu — cùng thủ thuật AppendBytesObfuscate mà server dùng cho
     * response, và ap-login gỡ bằng deobfuscate.
     */
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

    /**
     * Server tách theo dấu phân cách rồi giải từng khối, nên kích thước khối do phía client
     * quyết. Lưu ý: RsaEncryption.ts đặt maxLength = 245 (công thức PKCS#1) nhưng lại mã hoá
     * bằng OAEP, vốn chỉ chứa nổi 214 byte với khoá 2048-bit. Chia nhỏ hơn cho chắc.
     */
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
            // Dấu phân cách đứng sau MỌI khối, kể cả khối cuối: bản decrypt bỏ qua phần rỗng.
            out.append(rsaDelimiter)
            offset += size
        }
        return out.toString()
    }

    companion object {
        private const val OBFUSCATE_BYTES = 16
        private const val CHUNK_BYTES = 190

        // forge encrypt(..., "RSA-OAEP") mặc định SHA-1 cho cả digest lẫn MGF1.
        private const val RSA_OAEP_SHA1 = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding"
    }
}
