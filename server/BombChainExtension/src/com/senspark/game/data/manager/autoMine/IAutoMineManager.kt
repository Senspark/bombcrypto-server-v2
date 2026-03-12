package com.senspark.game.data.manager.autoMine

import com.google.gson.JsonArray
import com.senspark.common.service.IGlobalService
import com.senspark.game.data.model.config.AutoMinePackage
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.House
import com.senspark.game.declare.EnumConstants

interface IAutoMineManager : IGlobalService {
    val listConfigPackages: Map<EnumConstants.DataType, List<AutoMinePackage>>
    fun setConfig(listPackage: Map<EnumConstants.DataType, List<AutoMinePackage>>)
    fun toJsonArray(dataType: EnumConstants.DataType): JsonArray
    fun valueOf(namePackage: String, dataType: EnumConstants.DataType): AutoMinePackage?
    fun getMaxOfflineTimeWithNoAutoMine(dataType: EnumConstants.DataType): Double
    fun calculateOfflineReward(timeOffline: Double, heroes: List<Hero>, house: House?, dataType: EnumConstants.DataType, enableAutoMine: Boolean): Double
}