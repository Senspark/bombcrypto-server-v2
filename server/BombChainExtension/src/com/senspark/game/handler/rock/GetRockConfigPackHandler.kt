package com.senspark.game.handler.rock

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.rock.IBuyRockManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class GetRockConfigPackHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_ROCK_PACK_CONFIG_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val buyRockManager = controller.svServices.get<IBuyRockManager>()
            val rockPackConfig = buyRockManager.getListPackage()
            val result = SFSArray()

            rockPackConfig.forEach {
                val sfsObject = SFSObject()
                sfsObject.putUtfString("pack_name", it.getName())
                sfsObject.putInt("rock_amount", it.getRockAmount())
                sfsObject.putDouble("sen_price", it.getSenPrice())
                sfsObject.putDouble("bcoin_price", it.getBcoinPrice())
                result.addSFSObject(sfsObject)
            }

            val response: ISFSObject = SFSObject()
            response.putSFSArray("data", result)

            sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}