package com.senspark.game.api

import com.senspark.game.manager.IEnvManager
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

interface ITelegramBotApi {
    fun sendMessage(uid: Int, message: String, scheduleAt: Instant?)
    fun cancelAllMessages(uid: Int)
}

class TelegramBotApi(
    private val _api: IRestApi,
    private val _envManager: IEnvManager
) : ITelegramBotApi {

    private val _sendMsgUrl = "${_envManager.apLoginPath}/telegram/bot/send_message"
    private val _cancelMsgUrl = "${_envManager.apLoginPath}/telegram/bot/cancel_message"
    private val _bearerToken = _envManager.apLoginToken

    override fun sendMessage(uid: Int, message: String, scheduleAt: Instant?) {
        try {
            val body = buildJsonObject {
                put("uid", uid)
                put("message", message)
                if (scheduleAt != null) put("scheduleAt", scheduleAt.toEpochMilli())
            }
            _api.post(_sendMsgUrl, _bearerToken, body)
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun cancelAllMessages(uid: Int) {
        try {
            val body = buildJsonObject {
                put("uid", uid)
            }
            _api.post(_cancelMsgUrl, _bearerToken, body)
        } catch (e: Exception) {
            // ignore
        }
    }
}