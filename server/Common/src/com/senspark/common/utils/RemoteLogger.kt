package com.senspark.common.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.fluentd.logger.FluentLogger
import java.util.Base64

enum class AppStage {
    LOCAL, TEST, PROD;

    companion object {
        fun fromString(value: String): AppStage {
            return when (value.lowercase()) {
                "local" -> LOCAL
                "test" -> TEST
                "prod" -> PROD
                else -> throw IllegalArgumentException("Unknown stage: $value")
            }
        }

        fun toString(stage: AppStage): String {
            return when (stage) {
                LOCAL -> "local"
                TEST -> "test"
                PROD -> "prod"
            }
        }
    }
}

data class RemoteLoggerInitData(
    val serviceName: String,
    val instanceId: String?,
    val stage: AppStage,
    val remoteHost: String,
)

class RemoteLogger(
    initData: RemoteLoggerInitData,
    zoneName: String?,
    allowFallback: Boolean = true,
) : ILogger, IGlobalLogger, IServerLogger {

    private val _logger = mutableListOf<ILogger>()
    private val _scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        val h = initData.remoteHost.split(":")
        val host = h.first()
        if (initData.stage == AppStage.LOCAL) {
            _logger.add(PrintLogger())
        }
        if (host != "localhost") {
            val port = if (h.size > 1) {
                h[1].toInt()
            } else 0
            if (port > 0) {
                _logger.add(InternalRemoteLogger(initData, zoneName, host, port))
            }
        }
    }

    override fun log2(visibleTag: String, compressibleMessage: String, customColor: ColorCode) {
        _scope.launch {
            _logger.forEach { it.log2(visibleTag, compressibleMessage, customColor) }
        }
    }

    override fun log2(visibleTag: String, compressibleMessage: () -> String, customColor: ColorCode) {
        _scope.launch {
            _logger.forEach { it.log2(visibleTag, compressibleMessage, customColor) }
        }
    }

    override fun log(message: String, customColor: ColorCode) {
        _scope.launch {
            _logger.forEach { it.log(message, customColor) }
        }
    }

    override fun warn(message: String) {
        _scope.launch {
            _logger.forEach { it.warn(message) }
        }
    }

    override fun error(message: String) {
        _scope.launch {
            _logger.forEach { it.error(message) }
        }
    }

    override fun error(ex: Exception) {
        _scope.launch {
            _logger.forEach { it.error(ex) }
        }
    }

    override fun error(prefix: String, ex: Exception) {
        _scope.launch {
            _logger.forEach { it.error(prefix, ex) }
        }
    }

    override fun initialize() {
    }
}

private object StaticRemoteLogger {
    private lateinit var _logger: FluentLogger

    fun getLogger(tag: String, host: String, port: Int): FluentLogger {
        if (!::_logger.isInitialized) {
            _logger = FluentLogger.getLogger(tag, host, port)
            println("Use FluentLogger with tag=${tag} host=${host} port=${port}")
        }
        return _logger
    }
}

private class InternalRemoteLogger(
    initData: RemoteLoggerInitData,
    zoneName: String?,
    host: String,
    port: Int,
) : ILogger {
    private val _logMsgConstructor = LogMessageConstructor(initData, zoneName)
    private val _logger = StaticRemoteLogger.getLogger("service.${_logMsgConstructor.tag}", host, port)

    companion object {
        private const val MAX_MESSAGE_LENGTH = 1000
        private const val EVENT_TYPE = "event"
    }

    override fun log2(visibleTag: String, compressibleMessage: String, customColor: ColorCode) {
        val msg = if (compressibleMessage.length > MAX_MESSAGE_LENGTH) {
            compressibleMessage.toByteArray(Charsets.UTF_8).let { bytes ->
                val outputStream = java.io.ByteArrayOutputStream()
                java.util.zip.GZIPOutputStream(outputStream).use { gzip ->
                    gzip.write(bytes)
                }
                Base64.getEncoder().encodeToString(outputStream.toByteArray())
            }
        } else {
            compressibleMessage
        }
        _logger.log(EVENT_TYPE, _logMsgConstructor.info("$visibleTag $msg"))
    }

    override fun log2(visibleTag: String, compressibleMessage: () -> String, customColor: ColorCode) {
        val msg = compressibleMessage()
        log2(visibleTag, msg, customColor)
    }

    override fun log(message: String, customColor: ColorCode) {
        _logger.log(EVENT_TYPE, _logMsgConstructor.info(message))
    }

    override fun warn(message: String) {
        _logger.log(EVENT_TYPE, _logMsgConstructor.warn(message))
    }

    override fun error(message: String) {
        _logger.log(EVENT_TYPE, _logMsgConstructor.error(message))
    }

    override fun error(ex: Exception) {
        _logger.log(EVENT_TYPE, _logMsgConstructor.error(null, ex))
    }

    override fun error(prefix: String, ex: Exception) {
        _logger.log(EVENT_TYPE, _logMsgConstructor.error(prefix, ex))
    }
}

/**
 * Cấu trúc log tag: serviceName.stage.instanceId?
 * Ví dụ:
 * - sv-bomb.local.sv-game-v1
 * - sv-bomb.test.sv-game-v2
 * - sv-bomb.prod.sv-pvp-v1
 * - ap-login.test
 *
 * Service name: tag
 */
private class LogMessageConstructor(
    initData: RemoteLoggerInitData,
    private val zoneName: String?
) {
    val tag: String
    private val _stageName = initData.stage.name.lowercase()
    private val instanceId = initData.instanceId

    init {
        var tag = "${initData.serviceName}.${_stageName}"
        if (instanceId != null) {
            tag += ".$instanceId"
        }
        this.tag = tag
    }

    fun error(errorMessage: String): Map<String, String> {
        return constructLog(errorMessage, "error")
    }

    fun error(prefix: String?, exception: Exception): Map<String, String> {
        val stackTrace = exception.stackTraceToString()
        val maxLines = 5
        val stackTraceLines = stackTrace.split("\n")
        val limitedStackTrace = stackTraceLines.take(maxLines).joinToString("\n")
        if (prefix != null) {
            val errorMessage = "$prefix: ${exception.message}\n$limitedStackTrace"
            return error(errorMessage)
        }
        return error(limitedStackTrace)
    }

    fun info(message: String): Map<String, String> {
        return constructLog(message, "info")
    }

    fun warn(message: String): Map<String, String> {
        return constructLog(message, "warn")
    }

    private fun constructLog(message: String, level: String): Map<String, String> {
        val data = mutableMapOf(
            "message" to message,
            "level" to level,
            "service" to tag,
            "stage" to _stageName,
        )
        if (instanceId != null) {
            data["instance_id"] = instanceId
        }
        if (zoneName != null) {
            data["zone"] = zoneName
        }
        return data
    }
}