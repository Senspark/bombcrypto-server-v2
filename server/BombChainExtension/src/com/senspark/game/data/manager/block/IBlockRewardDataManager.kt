package com.senspark.game.data.manager.block

import com.senspark.common.service.IGlobalService
import com.senspark.game.data.model.config.IBlockReward
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.DataType

data class BlockRewardOption(
    val type: EnumConstants.BLOCK_REWARD_TYPE,
    val weight: Int,
    val value: Float,
)

interface IBlockRewardDataManager : IGlobalService {
    fun setConfig(blockRewards: HashMap<DataType, HashMap<Int, MutableList<IBlockReward>>>)

    fun getRewards(
        dataType: DataType,
        rewardTypeRandom: List<EnumConstants.BLOCK_REWARD_TYPE>,
        blockType: Int
    ): List<IBlockReward>

    fun dumpRewards(): String

    // Reward table keyed by block type, sent to MapService on session init (it has no DB).
    fun getRewardOptionsSnapshot(dataType: DataType): Map<Int, List<BlockRewardOption>>
}