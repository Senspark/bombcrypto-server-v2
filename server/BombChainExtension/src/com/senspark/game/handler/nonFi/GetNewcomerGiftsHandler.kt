package com.senspark.game.handler.nonFi

import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.newUserGift.INewUserGiftManager
import com.senspark.game.declare.KickReason
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class GetNewcomerGiftsHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_NEWCOMER_GIFTS_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.checkHash()) {
            controller.disconnect(KickReason.CHEAT_LOGIN)
            return
        }
        return try {
            val newUserGiftManager = controller.svServices.get<INewUserGiftManager>()
            val gifts = newUserGiftManager.takeNewUserAndAddGifts(controller)
            controller.masterUserManager.userIAPShopManager.saveUserIapPack()
            val response = SFSObject.newInstance().apply {
                putSFSArray("items", gifts.sortedBy { it.item.id }.toSFSArray {
                    SFSObject().apply {
                        putInt("item_id", it.item.id)
                        putInt("quantity", it.quantity)
                        putInt("step", it.step)
                    }
                })
            }
            val blockRewardManager = controller.masterUserManager.blockRewardManager
            blockRewardManager.loadUserBlockReward()
            response.putSFSArray("rewards", blockRewardManager.toSfsArrays())
            
            controller.masterUserManager.heroTRManager.loadHero(loadImmediately = true)
            return sendSuccess(controller, requestId, response)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}