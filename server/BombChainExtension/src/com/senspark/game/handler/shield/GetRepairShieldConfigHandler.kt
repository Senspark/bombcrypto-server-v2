package com.senspark.game.handler.shield

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.hero.IHeroRepairShieldDataManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class GetRepairShieldConfigHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_REPAIR_SHIELD_CONFIG_V2

    /**
     * FIXME: bad optimization
     */
    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val heroRepairShieldDataManager = controller.svServices.get<IHeroRepairShieldDataManager>()
            val repairShieldConfig = heroRepairShieldDataManager.getPriceConfig()
            val result = SFSArray()

            repairShieldConfig.forEach { (rarity, shieldLevelMap) ->
                val array = SFSArray()
                shieldLevelMap.forEach { (shieldLevel, heroRepairShield) ->
                    val item = SFSObject()
                    item.putInt("rarity", rarity)
                    item.putInt("shield_level", shieldLevel)
                    item.putFloat("price", heroRepairShield.price)
                    item.putFloat("price_rock", heroRepairShield.priceRock)
                    array.addSFSObject(item)
                }
                result.addSFSArray(array)
            }

            val response: ISFSObject = SFSObject()
            response.putSFSArray("data", result)

            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }
}