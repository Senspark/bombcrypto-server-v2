package com.senspark.game.data.manager.autoMine

import com.google.gson.JsonArray
import com.senspark.game.data.model.config.AutoMinePackage
import com.senspark.game.data.model.config.OfflineRewardTHModeConfig
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.House
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.EnumConstants.DataType

class AutoMineManager(
    private val _shopDataAccess: IShopDataAccess,
    private val _gameDataAccess: IGameDataAccess
) : IAutoMineManager {

    override var listConfigPackages: MutableMap<DataType, List<AutoMinePackage>> = mutableMapOf()
    private val _listConfigOfflineReward: MutableMap<DataType, OfflineRewardTHModeConfig> = mutableMapOf()

    override fun initialize() {
        listConfigPackages.putAll(_shopDataAccess.loadAutoMinePackageConfig())
        _listConfigOfflineReward.putAll(_gameDataAccess.getOfflineRewardTHModeConfigs())
    }

    override fun setConfig(listPackage: Map<DataType, List<AutoMinePackage>>) {
        listConfigPackages.clear()
        listConfigPackages.putAll(listPackage)
    }

    override fun toJsonArray(dataType: DataType): JsonArray {
        val objArray = JsonArray()
        for (configPackage in listConfigPackages[dataType]!!) {
            val obj = configPackage.toJsonObject()
            objArray.add(obj)
        }

        return objArray
    }

    override fun valueOf(namePackage: String, dataType: DataType): AutoMinePackage? {
        return listConfigPackages[dataType]?.find { it.name == namePackage }
    }

    override fun getMaxOfflineTimeWithNoAutoMine(dataType: DataType): Double {
        return _listConfigOfflineReward[dataType]!!.noAutoMine
    }

    override fun calculateOfflineReward(
        timeOffline: Double,
        heroes: List<Hero>,
        house: House?,
        dataType: DataType,
        enableAutoMine: Boolean
    ): Double {
        if (timeOffline == 0.0) {
            return 0.0
        }
        val config = _listConfigOfflineReward[dataType]!!

        val rewardOffline = CalculateRewardOffline.calculate(heroes, house, config, timeOffline)
        val listReward = mutableListOf<Double>()
        var timeOfflineLeft = timeOffline
        /*
                1 tiếng đầu nhận 60% reward offline
                1 tiếng tiếp theo nhận 40% reward offline
                46 tiếng tiếp theo nhận 10% reward offline
                Các tiếng tiếp theo không nhận được gì
        */
        val percentReceivedReward = mutableMapOf(
            0.6f to 60,
            0.4f to 60,
            0.1f to 2760,
            0f to Int.MAX_VALUE
        )
        for ((percent, timeMinutes) in percentReceivedReward) {
            if (timeOfflineLeft > timeMinutes) {
                listReward.add(rewardOffline * percent * (timeMinutes / timeOffline))
                timeOfflineLeft -= timeMinutes
            } else {
                listReward.add(rewardOffline * percent * (timeOfflineLeft / timeOffline))
                break
            }
        }

        return listReward.sum()
    }
}