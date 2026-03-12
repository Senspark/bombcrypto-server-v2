package com.senspark.game.pvp.handler

import com.senspark.common.pvp.IMatchController
import com.senspark.game.pvp.HandlerCommand
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class UseEmojiHandler(
    private val _controller: IMatchController,
) : BasePVPEncryptRequestHandler() {
    override val command = HandlerCommand.UseEmoji
    override fun handleGameClientRequest(user: User, data: ISFSObject) {
        val response = SFSObject().apply {
            putUtfString("task_id", data.getUtfString("task_id"))
        }
        try {
            val itemId = data.getInt("item_id")
            _controller.useEmoji(user, itemId)
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