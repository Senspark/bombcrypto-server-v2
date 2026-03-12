package com.senspark.game.data.manager.treassureHunt

import com.senspark.common.service.IGlobalService
import com.senspark.game.data.model.config.TreasureHuntDataConfig
import com.senspark.game.declare.EnumConstants

interface ITreasureHuntConfigManager : IGlobalService {
    fun getDataConfig(): TreasureHuntDataConfig
    fun setDataConfig(config: TreasureHuntDataConfig)
    fun getPriceHouse(dataType: EnumConstants.DataType): List<Float>
    fun getPriceHero(dataType: EnumConstants.DataType): Map<EnumConstants.BLOCK_REWARD_TYPE, Float>
    fun getHouseStat(dataType: EnumConstants.DataType): List<HouseStat>
    fun getFusionFeeConfig(dataType: EnumConstants.DataType): List<Double>
    fun getHeroLimit(dataType: EnumConstants.DataType): Int
    fun getTimeDisableBuyWithTokenNetwork(dataType: EnumConstants.DataType): Long
    fun getPriceHouseWithTokenNetwork(dataType: EnumConstants.DataType): List<Float>
}