package com.senspark.game.service

import com.senspark.common.utils.ILogger
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.EnumConstants.HeroType
import com.senspark.game.manager.hero.IUserHeroFiManager
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

typealias Dict = MutableMap<Int, IUserHeroFiManager>

@Serializable
data class HeroStakeData(
    @SerialName("uid")
    val uid: Int? = null,
    @SerialName("hero_id")
    val heroId: Int,
    @SerialName("stake_bcoin")
    val stakeBcoin: Double,
    @SerialName("stake_sen")
    val stakeSen: Double,
    @SerialName("network")
    val network: Int
)

class AllHeroesFiManager(
    private val _logger: ILogger,
    private val _gameDataAccess: IGameDataAccess
) : IAllHeroesFiManager {

    companion object {
        private const val TAG = "ALL_HERO_FI_MANAGER:"
    }

    private val _networkSubManagers: Map<DataType, Dict> = DataType.entries.associateWith { mutableMapOf() }
    private val jsonProcessor = Json { ignoreUnknownKeys = true }

    override fun initialize() {
    }

    override fun destroy() {
    }

    override fun processHeroStake(json: String) {
        val data = jsonProcessor.decodeFromString<HeroStakeData>(json)
        val dataType = when (data.network) {
            0 -> DataType.BSC
            1 -> DataType.POLYGON
            else -> throw Exception("Invalid network type")
        }
        // Fast path when client-triggered: uid hint avoids the O(n) sub-manager scan.
        // Falls back to the scan when the broadcast came from the scheduler (no uid).
        val heroFiManager = if (data.uid != null) {
            getSubManager(data.uid, dataType)?.takeIf { it.getHero(data.heroId, HeroType.FI) != null }
        } else {
            getSubManagerByHeroId(data.heroId, dataType)
        }
        val hero = heroFiManager?.getHero(data.heroId, HeroType.FI)

        if (hero != null) {
            heroFiManager.updateStakeAmountHeroes(hero, data.stakeBcoin, data.stakeSen)
        } else {
            _gameDataAccess.updateBomberStakeAmount(
                dataType,
                data.heroId,
                HeroType.FI.value,
                data.stakeBcoin,
                data.stakeSen
            )
        }
    }

    override fun addSubManager(userId: Int, dataType: DataType, heroFiManager: IUserHeroFiManager) {
        _logger.log("$TAG add $userId")
        _networkSubManagers[dataType]!![userId] = heroFiManager
    }

    override fun removeSubManager(userId: Int, dataType: DataType) {
        _logger.log("$TAG remove $userId")
        _networkSubManagers[dataType]?.remove(userId)
    }

    override fun getSubManager(userId: Int, network: DataType): IUserHeroFiManager? {
        return _networkSubManagers[network]?.get(userId)
    }

    private fun getSubManagerByHeroId(heroId: Int, network: DataType): IUserHeroFiManager? {
        val dict = _networkSubManagers[network] ?: return null
        for ((_, manager) in dict) {
            if (manager.getHero(heroId, HeroType.FI) != null) {
                return manager
            }
        }
        return null
    }
}