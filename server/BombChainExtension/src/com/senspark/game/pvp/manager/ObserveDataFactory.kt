package com.senspark.game.pvp.manager;

import com.senspark.common.pvp.IMatchInfo
import com.senspark.game.pvp.data.IMatchObserveData
import com.senspark.game.pvp.data.MatchObserveData
import com.senspark.game.pvp.delta.IMatchStateDelta

class ObserveDataFactory(
    private val _matchInfo: IMatchInfo,
) : IObserveDataFactory {
    private var _id = 0

    override fun generate(timestamp: Long, stateDelta: IMatchStateDelta): IMatchObserveData {
        return MatchObserveData(
            id = _id++,
            timestamp = timestamp,
            matchId = _matchInfo.id,
            heroDelta = stateDelta.hero,
            bombDelta = stateDelta.bomb,
            blockDelta = stateDelta.block,
        )
    }
}
