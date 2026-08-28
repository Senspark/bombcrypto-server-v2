package com.senspark.testclient

import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.EncryptionHelper
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import sfs2x.client.SmartFox
import sfs2x.client.core.SFSEvent
import sfs2x.client.requests.ExtensionRequest
import sfs2x.client.requests.LoginRequest
import sfs2x.client.util.ConfigData
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.SecretKey

/**
 * Client SmartFox tối giản cho test tích hợp: đăng nhập qua endpoint Editor, gọi lệnh và giải mã
 * phản hồi. Khớp phản hồi theo `rid` nên gọi cùng một lệnh nhiều lần không giẫm lên nhau.
 */
class SfsTestClient(
    private val host: String = TestEnv.optional("TEST_SFS_HOST", "localhost")!!,
    private val port: Int = TestEnv.optional("TEST_SFS_PORT", "9933")!!.toInt(),
    private val zone: String = TestEnv.optional("TEST_SFS_ZONE", "BomberGameZone")!!,
) {
    private val sfs = SmartFox()
    private val pending = ConcurrentHashMap<Int, CompletableFuture<ISFSObject>>()
    private val requestId = AtomicInteger(1)

    private lateinit var aesKey: SecretKey

    fun login(walletAddress: String, editorLogin: EditorLogin, network: String = "BSC") {
        val credentials = editorLogin.fetch(walletAddress)
        aesKey = editorLogin.aesKey

        connect()

        val loggedIn = CompletableFuture<Boolean>()
        sfs.addEventListener(SFSEvent.EXTENSION_RESPONSE) { event ->
            val cmd = event.arguments["cmd"] as String
            val params = event.arguments["params"] as ISFSObject
            // Phiên sẵn sàng ở USER_INITIALIZED, không phải ở sự kiện LOGIN.
            if (cmd == USER_INITIALIZED) {
                loggedIn.complete(true)
            } else {
                resolve(params)
            }
        }
        sfs.addEventListener(SFSEvent.LOGIN_ERROR) { event ->
            loggedIn.completeExceptionally(
                IllegalStateException("login failed: ${event.arguments["errorMessage"]}")
            )
        }

        val data = SFSObject().apply {
            putUtfString(SFSField.User_Name, walletAddress)
            putUtfString(SFSField.Slogan, "enter_game")
            putInt(SFSField.LoginType, LOGIN_TYPE_BNB_POL)
            putInt("version_code", 1)
            putUtfString(SFSField.DataType, network)
            putUtfString(SFSField.LoginTokenData, credentials.loginData)
            // MOBILE rẽ sang verifyMobileUser, một đường xác thực khác.
            putUtfString(SFSField.DeviceType, "WEB")
        }
        sfs.send(LoginRequest(walletAddress, "", zone, SFSObject().apply { putSFSObject("data", data) }))

        loggedIn.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    /** Gửi lệnh và trả về JSON đã giải mã. */
    fun send(command: String, payload: ISFSObject = SFSObject()): String {
        val rid = requestId.incrementAndGet()
        val future = CompletableFuture<ISFSObject>()
        pending[rid] = future

        val request = SFSObject().apply {
            putInt(SFSField.NewRequestId, rid)
            putUtfString(SFSField.Data, if (payload.size() == 0) "" else payload.toJson())
        }
        sfs.send(ExtensionRequest(command, request))

        val response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        response.getInt(SFSField.ErrorCode)?.let { code ->
            error("$command trả lỗi ec=$code ${response.getUtfString(SFSField.ErrorString).orEmpty()}")
        }
        val encrypted = response.getByteArray(SFSField.Data)
            ?: error("$command không có trường ${SFSField.Data}")
        return EncryptionHelper.decrypt(Base64.getEncoder().encodeToString(encrypted), aesKey)
    }

    fun close() {
        runCatching { sfs.disconnect() }
        sfs.removeAllEventListeners()
    }

    private fun connect() {
        val connected = CompletableFuture<Boolean>()
        sfs.addEventListener(SFSEvent.CONNECTION) { event ->
            connected.complete(event.arguments["success"] as Boolean)
        }
        sfs.connect(ConfigData().apply {
            host = this@SfsTestClient.host
            port = this@SfsTestClient.port
            zone = this@SfsTestClient.zone
            isDebug = false
        })
        check(connected.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "không kết nối được $host:$port" }
    }

    private fun resolve(params: ISFSObject) {
        val rid = params.getInt(SFSField.NewRequestId) ?: return
        pending.remove(rid)?.complete(params)
    }

    companion object {
        private const val USER_INITIALIZED = "USER_INITIALIZED"
        private const val LOGIN_TYPE_BNB_POL = 0
        private const val TIMEOUT_SECONDS = 30L
    }
}
