package com.senspark.game.pvp.handler

import com.senspark.common.pvp.IMatchController
import com.senspark.game.pvp.HandlerCommand
import com.smartfoxserver.v2.entities.User
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class QuitHandler(
    private val _controller: IMatchController,
) : BasePVPEncryptRequestHandler() {
    override val command = HandlerCommand.Quit
    override fun handleGameClientRequest(user: User, data: ISFSObject) {
        val response = SFSObject()
        try {
            _controller.quit(user)
            response.apply {
                putInt("code", 0)
            }
        } catch (ex: Throwable) {
            response.apply {
                putInt("code", 100)
                putUtfString("message", ex.message ?: "")
            }
        }
        sendSuccess(user, response)
    }
}