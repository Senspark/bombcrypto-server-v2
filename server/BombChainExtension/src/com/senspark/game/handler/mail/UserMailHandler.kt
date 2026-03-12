package com.senspark.game.handler.mail

import com.senspark.game.controller.LegacyUserController
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.room.BaseGameRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class UserMailHandler(
    val command: String
) : BaseGameRequestHandler() {
    override val serverCommand = command

    override fun handleGameClientRequest(controller: LegacyUserController, params: ISFSObject) {
        throw CustomException("[UserMailHandler] Command not supported")
        // nhanc18 disable MAIL feature
        /*return try {
            val manager = controller.masterUserManager.userMailManager
            when (command) {
                SFSCommand.GET_MAILS -> {
                    sendResponseToClient(manager.mails.toSFSArray { it.value.toSfsObject() }, controller)
                }


                SFSCommand.MARK_READ_MAIL -> {
                    val id = params.getUtfString("id")
                    manager.markRead(id)
                    sendSuccessClient(controller)
                }


                SFSCommand.CLAIM_MAIL_ATTACH -> {
                    val id = params.getUtfString("id")
                    val results = manager.claim(id)
                    sendResponseToClient(results.toSFSArray { it.toSfsObject() }, controller)
                }


                SFSCommand.CLAIM_ALL_MAIL_ATTACH -> {
                    val results = manager.claimAll()
                    sendResponseToClient(results.toSFSArray { it.toSfsObject() }, controller)
                }

                SFSCommand.DELETE_MAIL -> {
                    val id = params.getUtfString("id")
                    manager.delete(id)
                    sendSuccessClient(controller)
                }

                SFSCommand.DELETE_ALL_MAIL -> {
                    manager.deleteAll()
                    sendSuccessClient(controller)
                }

                else -> throw CustomException("[UserSubscriptionHandler] Command invalid")
            }
        } catch (ex: Exception) {
            sendMessageError(ex, controller)
        }*/
    }
}