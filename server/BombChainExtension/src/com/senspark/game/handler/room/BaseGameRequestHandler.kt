package com.senspark.game.handler.room

import com.senspark.common.service.IScheduler
import com.senspark.common.utils.AppStage
import com.senspark.common.utils.ILogger
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.declare.*
import com.senspark.game.exception.CustomException
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.handler.MainGameExtensionBaseRequestHandler
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.service.IHandlerLogger
import com.senspark.game.utils.DataClient
import com.senspark.game.utils.SignatureUtils.verifySignature
import com.senspark.game.utils.Utils
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.Zone
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.SQLException
import kotlin.math.abs

abstract class BaseGameRequestHandler : MainGameExtensionBaseRequestHandler() {

    companion object {
        private const val TICKS_AT_EPOCH = 62135596800000L
    }

    val zone: Zone get() = parentExtension.parentZone

    protected val factoryDataAccess = services.get<IDataAccessManager>()
    protected val envManager = services.get<IEnvManager>()
    protected val scheduler = services.get<IScheduler>()
    private val _usersManager: List<IUsersManager> = services.get<ISvServicesContainer>().filter(IUsersManager::class)
    private val _handlerLogger = services.get<IHandlerLogger>()
    private val _isServerTest = envManager.appStage != AppStage.PROD

    protected abstract val serverCommand: String
    private var requestId: Int = 0

    override fun handleClientRequest(user: User, params: ISFSObject) {
        val controller = _usersManager.firstNotNullOfOrNull { it.getUserController(user) } ?: return

        controller as LegacyUserController

        if (controller.isInitialized()) {
            handleMsgClientRequest(controller, params)
        }
    }

    private fun handleMsgClientRequest(controller: LegacyUserController, params: ISFSObject) {
        val data = params.getSFSObject("data")

        logRequest(controller, params, data)
        if (!controller.isActive) {
            return
        }
        if (controller.checkDdos()) {
            return
        }
        val requestId = params.getInt("id")
        val id = controller.requestId
        controller.increaseRequestId()

        if (id < 10 || id % GameConstants.VERIFY_SUPPLY == 0) {
            val timestamp = params.getLong("timestamp")
            val tick = System.currentTimeMillis() + TICKS_AT_EPOCH
            if (abs(tick - timestamp) >= 60000) { // Reduced tolerance to 60s (was 600s)
                controller.logger.error("$serverCommand ${controller.userName} invalid timestamp: $timestamp <> $tick")
                trustDisconnectUser(controller, KickReason.WRONG_TIMESTAMP)
                return
            }
            // nếu login = account sẽ dùng seconde username để làm hash
            if (envManager.appStage == AppStage.PROD && !verifySignature(
                    // Note: use second username will not worked for linked accounts.
                    controller.userName,
                    requestId,
                    serverCommand,
                    params
                )
            ) {
                controller.logger.error("$serverCommand ${controller.userName} invalid signature")
                trustDisconnectUser(controller, KickReason.CHEAT_INVALID_SIGNATURE)
                return
            }
        }
        this.requestId = requestId
        handleGameClientRequest(controller, data)
    }

    /**
     * Mặc định sẽ log ở DEBUG level đối với Server Production (để tiết kiệm chi phí).
     * Cho nên cần thiết xem logs thì sẽ xem ở smartfox.log
     * Server Test sẽ hiển thị những log này để dễ debug
     */
    private fun logRequest(
        controller: LegacyUserController,
        params: ISFSObject,
        data: ISFSObject
    ) {
        if (!envManager.logHandler) {
            return
        }
        val logData: String = if (data.size() != 0) params.getSFSObject("data").toJson() else ""
        _handlerLogger.log(controller, serverCommand, logData)
    }

    protected abstract fun handleGameClientRequest(
        controller: LegacyUserController,
        params: ISFSObject
    )

    protected fun sendErrorToClient(
        errorCode: Int,
        errorMessage: String?,
        controller: LegacyUserController
    ) {
        sendErrorCodeToClient(errorCode, errorMessage, 0, controller)
    }

    private fun sendErrorCodeToClient(
        errorCode: Int,
        errorString: String?,
        controller: LegacyUserController,
        vararg params: Any
    ) {
        sendErrorCodeToClient(errorCode, errorString, 0, controller, *params)
    }

    private fun sendErrorCodeToClient(
        errorCode: Int,
        errorString: String?,
        timestamp: Long,
        controller: LegacyUserController,
        vararg params: Any
    ) {
        val data = SFSObject()
        data.putInt(SFSField.ErrorCode, errorCode)
        data.putInt(SFSField.RequestId, requestId)
        data.putLong("timestamp", timestamp)
        if (errorString != null) {
            data.putUtfString(SFSField.ErrorString, errorString)
        }
        val locParams = SFSArray()
        data.putSFSArray(SFSField.LocParam, locParams)
        for (param in params) {
            locParams.addUtfString(param.toString())
        }
        controller.send(serverCommand, data)
    }

    protected fun sendResponseToClient(data: ISFSObject, controller: LegacyUserController) {
        if (controller.dataType == EnumConstants.DataType.SOL) {
            sendResponseToUserController(serverCommand, DataClient.convertResponse(data), controller)
        } else {
            sendResponseToUserController(serverCommand, data, controller)
        }
    }

    protected fun sendResponseToClient(data: ISFSArray?, controller: LegacyUserController) {
        val sfsObject: ISFSObject = SFSObject()
        sfsObject.putSFSArray("data", data)
        sendResponseToUserController(serverCommand, sfsObject, controller)
    }

    protected fun sendSuccessClient(legacyUserController: LegacyUserController) {
        sendResponseToUserController(serverCommand, SFSObject(), legacyUserController)
    }

    protected fun sendResponseToUserController(
        command: String?,
        data: ISFSObject,
        controller: LegacyUserController
    ) {
        if (command == null) {
            return
        }
        data.putInt(SFSField.RequestId, requestId)
        data.putInt(SFSField.ErrorCode, ErrorCode.SUCCESS)
        controller.send(command, data)
    }

    protected fun sendSuccessToClient(controller: LegacyUserController) {
        val sfsObject: ISFSObject = SFSObject()
        sfsObject.putNull("data")
        sendResponseToUserController(serverCommand, sfsObject, controller)
    }

    protected fun sendMessageError(
        errorCode: Int,
        controller: LegacyUserController,
        vararg params: Any?
    ) {
        val es = ""
        sendErrorCodeToClient(errorCode, es, controller, params)
    }

    protected fun sendMessageError(exception: CustomException, controller: LegacyUserController) {
        sendErrorCodeToClient(exception.code, exception.message, controller)
    }

    protected fun sendMessageError(exception: Exception, controller: LegacyUserController) {
        sendMessageError(exception, 0, controller)
    }

    protected fun sendMessageError(
        exception: Exception,
        controller: LegacyUserController,
        timestamp: Long
    ) {
        sendMessageError(exception, timestamp, controller)
    }

    protected fun sendMessageError(
        exception: Exception,
        timestamp: Long,
        legacyUserController: LegacyUserController
    ) {
        when (exception) {
            is CustomException -> {
                if (_isServerTest || exception.willTraceLog) {
                    parseExceptionContents(exception, legacyUserController.logger)
                }
                sendErrorCodeToClient(
                    exception.code,
                    exception.message,
                    timestamp,
                    legacyUserController
                )
            }

            is SQLException -> {
                val customException = Utils.parseSQLException(exception)
                if (_isServerTest || customException.willTraceLog) {
                    parseExceptionContents(exception, legacyUserController.logger)
                }
                sendErrorCodeToClient(
                    customException.code,
                    customException.message,
                    timestamp,
                    legacyUserController
                )
            }

            else -> {
                parseExceptionContents(exception, legacyUserController.logger)
                sendErrorCodeToClient(
                    ErrorCode.SERVER_ERROR,
                    exception.message,
                    timestamp,
                    legacyUserController
                )
            }
        }
    }

    protected fun parseExceptionContents(exception: Exception, logger: ILogger) {
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
        logger.error(sb.toString())
    }

    protected fun trustDisconnectUser(legacyUserController: LegacyUserController?, kickReason: KickReason) {
        legacyUserController?.disconnect(kickReason)
    }
}