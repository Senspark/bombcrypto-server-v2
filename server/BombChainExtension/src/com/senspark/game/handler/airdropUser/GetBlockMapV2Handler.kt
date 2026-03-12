package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class GetBlockMapV2Handler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_BLOCK_MAP_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val manager = controller.masterUserManager.userBlockMapManager
            synchronized(manager.locker) {
                val blockMap = controller.masterUserManager.userBlockMapManager.getBlockMap()
                sendSuccess(controller, requestId, blockMap)
            }
        } catch (e: CustomException) {
            sendExceptionError(controller, requestId, e)
        }
    }

}