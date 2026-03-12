package com.senspark.game.handler.gacha

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.user.GachaChestManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class GetGachaChestsHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_GACHA_CHESTS_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        try {
            val userGachaChestManager = controller.masterUserManager.userGachaChestManager
            val chests = userGachaChestManager.getAllChests()
            val responseData = SFSArray()
            chests.forEach {
                val sfsObject = SFSObject()
                sfsObject.putUtfString("slotType", it.slotType.name)
                sfsObject.putInt("slotNumber", it.slotNumber)
                sfsObject.putBool("isOwner", it.isOwner)
                sfsObject.putInt("price", it.price)
                it.chest?.let { chest ->
                    val chestConfig = chest.chestConfig
                    sfsObject.putSFSObject("chest", SFSObject.newInstance().apply {
                        putInt("chest_id", chest.id)
                        putInt("chest_type", chest.chestType.value)
                        putLong("remaining_time", chest.remainingOpenTime)
                        putLong("total_open_time", chestConfig.openTimeInMinute * 60 * 1000L)
                        putInt("skip_open_time_gem_require", chestConfig.skipOpenTimeGemRequire)
                        putLong("skip_time_per_ads", GachaChestManager.SKIP_TIME_PER_ADS_IN_MILLIS)
                    })
                }
                responseData.addSFSObject(sfsObject)
            }
            return sendSuccess(controller, requestId, SFSObject().apply { putSFSArray("data", responseData) })
        }
        catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }

}