package com.senspark.game.handler.sol

import com.senspark.common.service.IScheduler
import com.senspark.common.utils.AppStage
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.handler.MainGameExtensionBaseRequestHandler
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.utils.AesEncryption
import com.senspark.game.utils.Utils
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.SQLException
import java.security.SecureRandom
import java.util.*
import javax.crypto.SecretKey

abstract class BaseEncryptRequestHandler : MainGameExtensionBaseRequestHandler() {

    protected abstract val serverCommand: String

    protected val scheduler = services.get<IScheduler>()
    protected val coroutine: ICoroutineScope = services.get<ICoroutineScope>()
    private val _usersManager: List<IUsersManager> = services.get<ISvServicesContainer>().filter(IUsersManager::class)
    private val _isServerTest = services.get<IEnvManager>().appStage != AppStage.PROD

    override fun handleClientRequest(user: User, params: ISFSObject) {
        val controller = _usersManager.firstNotNullOfOrNull { it.getUserController(user) } ?: return
        if (controller.isInitialized()) {
            handleMsgClientRequest(controller, params)
        }
    }

    private fun handleMsgClientRequest(controller: IUserController, params: ISFSObject) {
        val requestId = params.getInt(SFSField.NewRequestId)
        val encryptedData = params.getUtfString(SFSField.Data)

        if (encryptedData.isEmpty()) {
            log(controller, false, "($requestId) <empty>")
            handleGameClientRequest(controller, requestId, SFSObject())
        } else {
            try {
                val decryptedData = EncryptionHelper.decrypt(encryptedData, controller.userInfo.aesKey)
                log(controller, false, "($requestId) $decryptedData")
                val data = SFSObject.newFromJsonData(decryptedData)
                handleGameClientRequest(controller, requestId, data)
            } catch (ex: Exception) {
                controller.logger.error("Decrypt error: ${controller.userId} ${controller.userName}", ex)
//                controller.disconnect(KickReason.KICK)
            }
        }
    }

    protected abstract fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject)

    protected fun sendExceptionError(userController: IUserController, requestId: Int, ex: Throwable) {
        when (ex) {
            is CustomException -> {
                if (_isServerTest || ex.willTraceLog) {
                    parseExceptionContents(ex, userController)
                }
                else {
                    userController.logger.error("Custom Exception: ${userController.userName}", ex)
                }
                sendError(userController, requestId, ex.code, ex.message)
            }

            is SQLException -> {
                val customException = Utils.parseSQLException(ex)
                if (_isServerTest || customException.willTraceLog) {
                    parseExceptionContents(ex, userController)
                }
                else {
                    userController.logger.error("SQL Exception: ${userController.userName}", ex)
                }
                sendError(userController, requestId, customException.code, "Handler server error")
            }

            is Exception -> {
                parseExceptionContents(ex, userController)
                sendError(userController, requestId, ErrorCode.SERVER_ERROR, "Handler server error")
            }
        }
    }

    protected fun sendError(
        userController: IUserController,
        requestId: Int,
        errorCode: Int,
        errorString: String?
    ) {
        val data = SFSObject()
        data.putInt(SFSField.NewRequestId, requestId)
        data.putInt(SFSField.ErrorCode, errorCode)
        data.putUtfString(SFSField.ErrorString, errorString ?: "")
        userController.send(serverCommand, data, false)
        log(userController, true, "($requestId) Error: $errorCode $errorString")
    }

    protected fun sendSuccess(
        userController: IUserController,
        requestId: Int,
        data: ISFSObject
    ) {
        val json = data.toJson()
        val encryptedData = EncryptionHelper.encryptToBytes(json, userController.userInfo.aesKey)
        val responseData = SFSObject()
        responseData.putInt(SFSField.NewRequestId, requestId)
        responseData.putByteArray(SFSField.Data, encryptedData)
        userController.send(serverCommand, responseData, false)
        log(userController, true, json)
    }

    private fun log(controller: IUserController, send: Boolean, msg: String) {
        val prefix = if (send) "[OUT]" else "[IN]"
        val tag = "$prefix ${controller.userId}-${controller.userName}: $serverCommand"
        controller.logger.log2(tag, msg)
    }

    protected fun parseExceptionContents(exception: Exception, controller: IUserController) {
        val message = exception.message
        val stackTrace = exception.stackTrace
        val sb = StringBuilder()
        sb.append("Error: ").append(message).append("\r\n")
        for (i in stackTrace.indices) {
            val exceptionMsg = String.format(
                "Root %s. Exception thrown from %s in class %s on line number %s of file %s",
                i, stackTrace[i].methodName, stackTrace[i].className, stackTrace[i].lineNumber, stackTrace[i].fileName
            )
            sb.append(exceptionMsg).append("\r\n")
        }
        controller.logger.error(sb.toString())
    }
}

object EncryptionHelper {
    private val _obfuscateHelper = AppendBytesObfuscate(16)

    fun encrypt(plainText: String, secret: SecretKey): String {
        return _obfuscateHelper.obfuscate(AesEncryption.encrypt(plainText, secret))
    }

    fun encryptToBytes(plainText: String, secret: SecretKey): ByteArray {
        val encrypted = AesEncryption.encryptToBytes(plainText, secret)
        return _obfuscateHelper.obfuscateToBytes(encrypted)
    }

    fun decrypt(plainText: String, secret: SecretKey): String {
        return AesEncryption.decrypt(_obfuscateHelper.deobfuscate(plainText), secret)
    }
}

class AppendBytesObfuscate(
    private val _numberOfBytes: Int
) {
    fun obfuscate(base64: String): String {
        return obfuscate(Base64.getDecoder().decode(base64))
    }

    fun obfuscate(bytes: ByteArray): String {
        return Base64.getEncoder().encodeToString(obfuscateToBytes(bytes))
    }

    fun obfuscateToBytes(bytes: ByteArray): ByteArray {
        val newBytes = ByteArray(bytes.size + _numberOfBytes)
        val randomBytes = ByteArray(_numberOfBytes)
        SecureRandom().nextBytes(randomBytes)
        System.arraycopy(randomBytes, 0, newBytes, 0, _numberOfBytes)
        System.arraycopy(bytes, 0, newBytes, _numberOfBytes, bytes.size)
        return newBytes
    }

    fun deobfuscate(base64: String): String {
        return Base64.getEncoder().encodeToString(deobfuscateToBytes(base64))
    }

    fun deobfuscateToBytes(base64: String): ByteArray {
        val bytes = Base64.getDecoder().decode(base64)
        return bytes.copyOfRange(_numberOfBytes, bytes.size)
    }
}

