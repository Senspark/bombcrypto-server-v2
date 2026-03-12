package com.senspark.game.pvp.handler

import com.senspark.common.pvp.IMatchController
import com.senspark.game.pvp.HandlerCommand
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class UseBoosterHandler(
    private val _scope: CoroutineScope,
    private val _controller: IMatchController,
) : BasePVPEncryptRequestHandler() {
    override val command = HandlerCommand.UseBooster

    override fun handleGameClientRequest(user: User, data: ISFSObject) {
        _scope.launch {
            val response = SFSObject().apply {
                putUtfString("task_id", data.getUtfString("task_id"))
            }
            try {
                val timestamp = data.getLong("timestamp")
                val itemId = data.getInt("item_id")
                _controller.useBooster(user, timestamp, itemId)
                response.apply {
                    putInt("code", 0)
                }
            } catch (ex: Exception) {
                logger.error(ex)
                response.apply {
                    putInt("code", 100)
                    putUtfString("message", ex.message ?: "")
                }
            }
            sendSuccess(user, response)
        }
    }
}