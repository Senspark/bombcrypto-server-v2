package com.senspark.game.handler.newMarket

import com.senspark.game.constant.ItemType
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.dailyTask.DailyTaskManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class BuyItemMarketHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.BUY_ITEM_MARKET_V3

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        return try {
            
            val itemBuy  = controller.masterUserManager.userMarketplaceManager.buyV3(controller.userId)
            
            // User có mua item từ fixed pool => add item vào inventory
            if(itemBuy.fixedQuantity > 0) {
                val invManager = controller.masterUserManager.userInventoryManager
                invManager.addItem(
                    itemId = itemBuy.itemId,
                    quantity = itemBuy.fixedQuantity,
                    expiration = itemBuy.expiration.toLong() * 1000, // to milliseconds
                    reason = "Buy market item from fixed pool"
                )
            }

            if (itemBuy.itemType == ItemType.HERO.value) {
                controller.masterUserManager.userDailyTaskManager.updateProgressTask(DailyTaskManager.BuyHeroP2P)
                controller.masterUserManager.heroTRManager.loadHero(true)
            }
            else{
                controller.masterUserManager.userDailyTaskManager.updateProgressTask(DailyTaskManager.BuyItemP2P, itemBuy.totalQuantity)
            }
            val response = SFSObject()
            controller.saveGameAndLoadReward()
            response.putSFSArray("rewards", controller.masterUserManager.blockRewardManager.toSfsArrays())
            
            return sendSuccess(
                controller,
                requestId,
                response)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}