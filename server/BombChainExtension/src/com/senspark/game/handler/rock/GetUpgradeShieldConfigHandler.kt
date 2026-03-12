package com.senspark.game.handler.rock

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.service.IHeroUpgradeShieldManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class GetUpgradeShieldConfigHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_UPGRADE_SHIELD_CONFIG_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val heroUpgradeShieldManager = controller.svServices.get<IHeroUpgradeShieldManager>()
            val rockPackConfig = heroUpgradeShieldManager.getData()
            val result = SFSArray()

            rockPackConfig.forEach {
                val sfsObject = SFSObject()
                sfsObject.putInt("rarity", it.rarity)
                sfsObject.putUtfString("durability_point", it.values.toString())
                sfsObject.putUtfString("price_rock", it.prices.toString())
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