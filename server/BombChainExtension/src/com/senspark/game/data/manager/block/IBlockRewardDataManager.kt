package com.senspark.game.data.manager.block

import com.senspark.common.service.IGlobalService
import com.senspark.game.data.model.config.IBlockReward
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.DataType

interface IBlockRewardDataManager : IGlobalService {
    fun setConfig(blockRewards: HashMap<DataType, HashMap<Int, MutableList<IBlockReward>>>)

    fun getRewards(
        dataType: DataType,
        rewardTypeRandom: List<EnumConstants.BLOCK_REWARD_TYPE>,
        blockType: Int
    ): List<IBlockReward>

    fun dumpRewards(): String
}