package com.senspark.game.manager.heroCage

import com.senspark.common.utils.ILogger
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.exception.CustomException
import com.senspark.lib.data.manager.IGameConfigManager
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class HeroCageRewardManager(
    private val _gameConfigManager: IGameConfigManager,
    private val _rewardDataAccess: IRewardDataAccess,
    private val _logger: ILogger,
) : IHeroCageRewardManager {

    companion object {
        private const val OFFER_TTL_MS = 5 * 60 * 1000L
        private val NETWORKS = setOf(DataType.BSC, DataType.POLYGON)
    }

    private class Offer(val expiresAt: Long, val network: DataType)

    private val _offers = ConcurrentHashMap<Int, Offer>()

    override fun initialize() {
    }

    override fun roll(userInfo: IUserInfo): Boolean {
        // PvP/Adventure sessions are forced to TR; the login network survives in originalDataType.
        val network = userInfo.originalDataType ?: userInfo.dataType
        if (network !in NETWORKS) {
            return false
        }
        if (Random.nextFloat() >= _gameConfigManager.heroCageRate) {
            return false
        }
        _offers[userInfo.id] = Offer(System.currentTimeMillis() + OFFER_TTL_MS, network)
        _logger.log("[HeroCage] offer uid=${userInfo.id} network=$network")
        return true
    }

    override fun claim(uid: Int): DataType {
        val offer = _offers.remove(uid)
        if (offer == null) {
            _logger.log("[HeroCage] claim without offer uid=$uid")
            throw CustomException("Reward not found")
        }
        if (offer.expiresAt < System.currentTimeMillis()) {
            _logger.log("[HeroCage] claim expired uid=$uid")
            throw CustomException("Reward expired")
        }
        _rewardDataAccess.addUserBlockReward(
            uid, BLOCK_REWARD_TYPE.BOMBERMAN, offer.network, 1f, reason = ChangeRewardReason.HERO_CAGE
        )
        _logger.log("[HeroCage] claimed uid=$uid network=${offer.network}")
        return offer.network
    }
}
