package com.senspark.game.pvp.handler

import com.senspark.common.pvp.IMatchController
import com.senspark.game.pvp.HandlerCommand
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MoveHeroHandler(
    private val _scope: CoroutineScope,
    private val _controller: IMatchController,
) : BasePVPEncryptRequestHandler() {
    override val command = HandlerCommand.MoveHero
    override fun handleGameClientRequest(user: User, data: ISFSObject) {
        _scope.launch {
            val response = SFSObject().apply {
                putUtfString("task_id", data.getUtfString("task_id"))
            }
            try {
                val timestamp = data.getLong("timestamp")
                val x = data.getFloat("x")
                val y = data.getFloat("y")
                val result = _controller.moveHero(user, timestamp, x, y)
                response.apply {
                    putFloat("x", result.x)
                    putFloat("y", result.y)
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