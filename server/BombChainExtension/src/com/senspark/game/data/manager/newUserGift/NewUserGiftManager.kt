package com.senspark.game.data.manager.newUserGift

import com.senspark.game.constant.ItemType
import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.model.config.NewUserGift
import com.senspark.game.data.model.user.AddUserItemWrapper
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.db.IShopDataAccess
import com.senspark.lib.data.manager.IGameConfigManager

class NewUserGiftManager(
    private val _shopDataAccess: IShopDataAccess,
    private val _rewardDataAccess: IRewardDataAccess,
    private val _configItemManager: IConfigItemManager,
    private val _configHeroTraditionalManager: IConfigHeroTraditionalManager,
    private val _gameConfigManager: IGameConfigManager,
) : INewUserGiftManager {

    private val newUserGifts: MutableList<NewUserGift> = mutableListOf()

    override fun initialize() {
        newUserGifts.addAll(_shopDataAccess.loadNewUserGift(_configItemManager))
    }

    override fun takeNewUserAndAddGifts(userController: IUserController): List<NewUserGift> {
        require(userController.masterUserManager.heroTRManager.canGetFreeHeroTR()) { "User's not a newcomer" }
        val equipItemIds = _gameConfigManager.newUserGiftSkin
        val rewardReceive = newUserGifts.map {
            AddUserItemWrapper(
                it.item,
                it.quantity,
                userId = userController.userId,
                configHeroTraditional = _configHeroTraditionalManager,
                expirationAfter = it.expirationAfter
            )
        }
        val equipItem = rewardReceive.filter { equipItemIds.contains(it.item.id) }
        require(equipItem.size == equipItemIds.size) {
            "New user gift to equip is invalid"
        }
        _rewardDataAccess.addTRRewardForUser(userController.userId,userController.dataType, rewardReceive, { reloadRewards(userController) }, "New_user_gift")
        userController.masterUserManager.userInventoryManager.loadInventory()
        for (item in equipItem) {
            userController.masterUserManager.userInventoryManager.activeSkinChest(
                item.item.type,
                mapOf(item.item.id to item.expirationAfter)
            )
        }
        val emojiIds = rewardReceive.filter { it.item.type == ItemType.EMOJI }.associate { 
            it.item.id to it.expirationAfter }
        userController.masterUserManager.userInventoryManager.activeSkinChest(
            ItemType.EMOJI,
            emojiIds
        )
        return newUserGifts
    }

    private fun reloadRewards(userController: IUserController) {
        userController.masterUserManager.blockRewardManager.loadUserBlockReward()
        userController.masterUserManager.heroTRManager.loadHero(true)
    }
}