package com.senspark.game.data.manager.treassureHunt

import com.senspark.game.data.model.config.TreasureHuntDataConfig
import com.senspark.game.db.ITHModeDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

@Serializable
class HouseStat(
    @SerialName("recovery") val recovery: Int,
    @SerialName("capacity") val capacity: Int
)

class TreasureHuntConfigManager(
    private val _thModeDataAccess: ITHModeDataAccess,
) : ITreasureHuntConfigManager {

    private lateinit var dataConfig: TreasureHuntDataConfig

    override fun initialize() {
        dataConfig = _thModeDataAccess.loadTreasureHuntDataConfig()
    }

    override fun getDataConfig(): TreasureHuntDataConfig {
        return dataConfig;
    }

    override fun setDataConfig(config: TreasureHuntDataConfig) {
        dataConfig = config
    }

    override fun getPriceHouse(dataType: EnumConstants.DataType): List<Float> {
        val config = dataConfig.dataConfigs[dataType]!!
        return Json.decodeFromString<List<Float>>(config.getUtfString("house_prices"))
    }

    override fun getPriceHero(dataType: EnumConstants.DataType): Map<BLOCK_REWARD_TYPE, Float> {
        val config = dataConfig.dataConfigs[dataType]!!
        val price = Json.decodeFromString<List<MutableMap<String, Float>>>(config.getUtfString("hero_price"))

        val result = mutableMapOf<BLOCK_REWARD_TYPE, Float>()
        price.forEach {
            it.forEach { reward ->
                try {
                    result[convertToRewardType(reward.key)] = reward.value
                } catch (_: Exception) {
                    // do nothing
                }
            }
        }
        if (dataType == EnumConstants.DataType.TON) {
            if (Instant.now().toEpochMilli() > getTimeDisableBuyWithTokenNetwork(dataType)) {
                result.remove(BLOCK_REWARD_TYPE.TON_DEPOSITED)
            }
        }
        if (dataType == EnumConstants.DataType.SOL) {
            if (Instant.now().toEpochMilli() > getTimeDisableBuyWithTokenNetwork(dataType)) {
                result.remove(BLOCK_REWARD_TYPE.SOL_DEPOSITED)
            }
        }
        result[BLOCK_REWARD_TYPE.BOMBERMAN] = 1f
        return result
    }

    override fun getHouseStat(dataType: EnumConstants.DataType): List<HouseStat> {
        val config = dataConfig.dataConfigs[EnumConstants.DataType.TON]!!
        return Json.decodeFromString<List<HouseStat>>(config.getUtfString("house_stats"))
    }

    override fun getFusionFeeConfig(dataType: EnumConstants.DataType): List<Double> {
        val config = dataConfig.dataConfigs[dataType]!!
        return Json.decodeFromString<List<Double>>(config.getUtfString("fusion_fee"))
    }

    override fun getHeroLimit(dataType: EnumConstants.DataType): Int {
        val config = dataConfig.dataConfigs[dataType]!!
        return config.getUtfString("hero_limit").toInt()
    }

    override fun getTimeDisableBuyWithTokenNetwork(dataType: EnumConstants.DataType): Long {
        val config = dataConfig.dataConfigs[dataType]!!
        return config.getUtfString("disable_buy_with_token_network").toLong()
    }

    override fun getPriceHouseWithTokenNetwork(dataType: EnumConstants.DataType): List<Float> {
        val config = dataConfig.dataConfigs[dataType]!!
        return Json.decodeFromString<List<Float>>(config.getUtfString("house_prices_token_network"))
    }

    private fun convertToRewardType(name: String): BLOCK_REWARD_TYPE {
        return when (name) {
            "bcoin_deposited" -> BLOCK_REWARD_TYPE.BCOIN_DEPOSITED
            "ton" -> BLOCK_REWARD_TYPE.TON_DEPOSITED
            "sol" -> BLOCK_REWARD_TYPE.SOL_DEPOSITED
            "ron" -> BLOCK_REWARD_TYPE.RON_DEPOSITED
            "bas" -> BLOCK_REWARD_TYPE.BAS_DEPOSITED
            "star_core" -> BLOCK_REWARD_TYPE.COIN
            "vic" -> BLOCK_REWARD_TYPE.VIC_DEPOSITED
            else -> throw Exception("Wrong type")
        }
    }
}