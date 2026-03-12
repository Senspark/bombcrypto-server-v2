package com.senspark.game.pvp.manager

import com.senspark.common.pvp.IMatchData
import com.senspark.common.pvp.IMatchResultInfo
import com.senspark.common.pvp.IMatchUserInfo
import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.data.IMatchObserveData
import com.senspark.game.pvp.data.IMatchStartData
import com.senspark.game.pvp.info.MatchHistoryInfo
import com.senspark.game.pvp.utility.JsonUtility

class DefaultReportManager(
    private val _logger: ILogger,
    private val _matchData: IMatchData,
    private val _userInfo: List<IMatchUserInfo>,
) : IReportManager {
    private val _json = JsonUtility.json
    private lateinit var _startData: IMatchStartData
    private val _observeData = mutableListOf<IMatchObserveData>()

    override val info
        get() = MatchHistoryInfo(
            id = _matchData.id,
            startTimestamp = _matchData.roundStartTimestamp,
            userInfo = _userInfo,
            startData = _startData,
            observeData = _observeData,
        )

    override fun start(data: IMatchStartData) {
        _startData = data
    }

    override fun observe(data: IMatchObserveData) {
        // FIXME: not used yet.
        // _observeData.add(data)
    }

    override fun finish(info: IMatchResultInfo) {
        // FIXME.
        // val str = _json.encodeToString(this.info)
        // _logger.log("report size=${str.length}")
        // _logger.log("report data=${str}")
    }
}