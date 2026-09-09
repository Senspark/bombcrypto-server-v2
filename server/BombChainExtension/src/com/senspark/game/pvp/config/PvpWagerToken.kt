package com.senspark.game.pvp.config

import com.senspark.game.declare.EnumConstants

enum class PvpWagerToken(
    val id: Int,
    val tokenType: EnumConstants.TokenType,
    val network: EnumConstants.DataType,
    val displayName: String,
    val rewardType: EnumConstants.BLOCK_REWARD_TYPE
) {
    BCOIN_BSC(1, EnumConstants.TokenType.BCOIN, EnumConstants.DataType.BSC, "BCOIN (BSC)", EnumConstants.BLOCK_REWARD_TYPE.BCOIN),
    BCOIN_POLYGON(2, EnumConstants.TokenType.BCOIN, EnumConstants.DataType.POLYGON, "BCOIN (Polygon)", EnumConstants.BLOCK_REWARD_TYPE.BCOIN),
    SEN_BSC(3, EnumConstants.TokenType.COIN, EnumConstants.DataType.BSC, "SEN (BSC)", EnumConstants.BLOCK_REWARD_TYPE.SENSPARK),
    SEN_POLYGON(4, EnumConstants.TokenType.COIN, EnumConstants.DataType.POLYGON, "SEN (Polygon)", EnumConstants.BLOCK_REWARD_TYPE.SENSPARK),
    NONE(0, EnumConstants.TokenType.BCOIN, EnumConstants.DataType.UNKNOWN, "None", EnumConstants.BLOCK_REWARD_TYPE.BCOIN);

    companion object {
        fun from(id: Int) = values().firstOrNull { it.id == id } ?: NONE
    }
}
