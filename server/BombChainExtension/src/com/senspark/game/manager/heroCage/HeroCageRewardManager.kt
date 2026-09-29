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

    // uid -> expiry (epoch ms).
    private val _offers = ConcurrentHashMap<Int, Long>()

    override fun initialize() {
    }

    override fun roll(userInfo: IUserInfo): Boolean {
        // PvP/Adventure sessions are forced to TR, so walletAddress is gone; this is the BSC/POLYGON wallet flag.
        if (!userInfo.isOriginallyFi) {
            return false
        }
        if (Random.nextFloat() >= _gameConfigManager.heroCageRate) {
            return false
        }
        _offers[userInfo.id] = System.currentTimeMillis() + OFFER_TTL_MS
        _logger.log("[HeroCage] offer uid=${userInfo.id}")
        return true
    }

    override fun claim(uid: Int, network: DataType) {
        if (network !in NETWORKS) {
            throw CustomException("Invalid network")
        }
        val expiresAt = _offers.remove(uid)
        if (expiresAt == null) {
            _logger.log("[HeroCage] claim without offer uid=$uid")
            throw CustomException("Reward not found")
        }
        if (expiresAt < System.currentTimeMillis()) {
            _logger.log("[HeroCage] claim expired uid=$uid")
            throw CustomException("Reward expired")
        }
        _rewardDataAccess.addUserBlockReward(
            uid, BLOCK_REWARD_TYPE.BOMBERMAN, network, 1f, reason = ChangeRewardReason.HERO_CAGE
        )
        _logger.log("[HeroCage] claimed uid=$uid network=$network")
    }
}
