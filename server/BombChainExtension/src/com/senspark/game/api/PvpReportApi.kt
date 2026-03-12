package com.senspark.game.api

import com.senspark.game.manager.IPvpEnvManager
import com.senspark.game.pvp.info.IMatchHistoryInfo

class PvpReportApi(envManager: IPvpEnvManager) : IPvpReportApi {
    override fun report(info: IMatchHistoryInfo) {
        // FIXME.
    }
}