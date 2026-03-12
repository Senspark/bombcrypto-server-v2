package com.senspark.game.pvp.user

import com.senspark.game.constant.Booster
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.senspark.game.pvp.manager.ITimeManager

class UserBooster(
    override val item: Booster,
    override val cooldown: Int,
    private val _quantity: Int,
    private val _timeManager: ITimeManager,
) : IUserBooster {
    private var _useTimes = 0
    private var _usedTimestamp = 0L

    override val useTimes get() = _useTimes
    override val usedTimestamp get() = _usedTimestamp

    override fun use() {
        if (useTimes >= _quantity) {
            throw CustomException("Booster is not enough", ErrorCode.NOT_ENOUGH_PVP_BOOSTER)
        }
        val timestamp = _timeManager.timestamp
        if (usedTimestamp + cooldown >= timestamp) {
            throw CustomException("Cool down invalid", ErrorCode.NOT_ENOUGH_PVP_BOOSTER)
        }
        _usedTimestamp = timestamp
        ++_useTimes
    }
}