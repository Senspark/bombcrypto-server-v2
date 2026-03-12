package com.senspark.game.manager.autoMine

import com.senspark.game.data.model.config.AutoMinePackage
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.House
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IUserAutoMineManager {

    fun startAutoMine(): ISFSObject
    fun buyPackage(autoMinePackage: AutoMinePackage, blockRewardType: BLOCK_REWARD_TYPE): ISFSObject
    fun packagePrice(): ISFSObject
    fun packagePriceUserAirdrop(dataType: DataType): ISFSObject
    fun getOfflineReward(heroes: List<Hero>, house: House?): ISFSObject
}