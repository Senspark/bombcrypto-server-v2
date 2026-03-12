package com.senspark.game.handler.rock

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.rock.IUserRockManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class GetBurnHeroConfigHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_BURN_HERO_CONFIG_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val userRockManager = controller.svServices.get<IUserRockManager>()
            val convertHeroRockConfig = userRockManager.convertHeroRockConfig
            val result = SFSArray()

            convertHeroRockConfig.forEach {
                val sfsObject = SFSObject()
                sfsObject.putInt("rarity", it.key)
                //Cho client cũ
                sfsObject.putFloat("rock", it.value.heroS)
                sfsObject.putFloat("heroS", it.value.heroS)
                sfsObject.putFloat("heroL", it.value.heroL)
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