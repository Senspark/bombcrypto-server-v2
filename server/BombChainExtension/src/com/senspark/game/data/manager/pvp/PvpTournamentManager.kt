package com.senspark.game.data.manager.pvp

import com.senspark.common.pvp.IPvpFixtureMatchInfo
import com.senspark.common.pvp.IRankManager
import com.senspark.game.data.manager.season.IPvpSeasonManager
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IShopDataAccess

class PvpTournamentManager(
    private val shopDataAccess: IShopDataAccess,
    private val rankManager: IRankManager,
    private val seasonManager: IPvpSeasonManager
) : IPvpTournamentManager {

    override fun initialize() {
    }

    override fun getMatchList(): List<IPvpFixtureMatchInfo> {
        return shopDataAccess.loadPvpFixture(rankManager, seasonManager.currentSeasonNumber)
    }
}