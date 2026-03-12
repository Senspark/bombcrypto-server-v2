package com.senspark.game.data.manager.pvp

import com.senspark.common.pvp.IPvpFixtureMatchInfo
import com.senspark.common.service.IServerService

interface IPvpTournamentManager : IServerService {
    fun getMatchList(): List<IPvpFixtureMatchInfo>
} 