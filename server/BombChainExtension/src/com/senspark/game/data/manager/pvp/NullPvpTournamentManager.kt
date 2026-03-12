package com.senspark.game.data.manager.pvp

import com.senspark.common.pvp.IPvpFixtureMatchInfo

class NullPvpTournamentManager : IPvpTournamentManager {

    override fun initialize() {
    }

    override fun getMatchList(): List<IPvpFixtureMatchInfo> {
        return emptyList()
    }
}