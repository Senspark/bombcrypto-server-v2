package com.senspark.game.data.model.config

import com.senspark.game.declare.EnumConstants

interface IBlockReward {
    val type: EnumConstants.BLOCK_REWARD_TYPE
    val weight: Int
    fun getValue(network: EnumConstants.DataType): Float
    fun dump(): String
}