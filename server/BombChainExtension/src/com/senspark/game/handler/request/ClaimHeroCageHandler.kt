package com.senspark.game.handler.request

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.heroCage.IHeroCageRewardManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClaimHeroCageHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.CLAIM_HERO_CAGE

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        coroutine.scope.launch(Dispatchers.IO) {
            try {
                val network = DataType.values().firstOrNull { it.name == data.getUtfString(SFSField.NETWORK) }
                    ?: throw CustomException("Invalid network")
                services.get<IHeroCageRewardManager>().claim(controller.userId, network)
                sendSuccess(controller, requestId, SFSObject())
            } catch (ex: Exception) {
                sendExceptionError(controller, requestId, ex)
            }
        }
    }
}
