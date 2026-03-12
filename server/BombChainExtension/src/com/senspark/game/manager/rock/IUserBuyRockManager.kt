package com.senspark.game.manager.rock

import com.senspark.game.data.model.config.RockPackage
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.smartfoxserver.v2.entities.data.ISFSArray

interface IUserBuyRockManager {
    fun buyPackage(rockPackage: RockPackage, blockRewardType: BLOCK_REWARD_TYPE)
}