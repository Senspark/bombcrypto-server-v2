package com.senspark.game.db

import com.senspark.common.IDatabase
import com.senspark.game.db.dailyMission.IMissionDataAccess
import com.senspark.game.db.gachaChest.IGachaChestDataAccess
import com.senspark.game.service.IPvpDataAccess
import com.senspark.lib.data.manager.GameConfigManager
import com.senspark.lib.db.ILibDataAccess

class DataAccessManager(
    override val database: IDatabase,
    override val shopDataAccess: IShopDataAccess,
    override val gameDataAccess: IGameDataAccess,
    override val logDataAccess: ILogDataAccess,
    override val userDataAccess: IUserDataAccess,
    override val rewardDataAccess: IRewardDataAccess,
    override val libDataAccess: ILibDataAccess,
    override val pvpDataAccess: IPvpDataAccess,
    override val gachaChestDataAccess: IGachaChestDataAccess,
    override val iapDataAccess: IIapDataAccess,
    override val tournamentDataAccess: IPvpTournamentDataAccess,
    override val missionDataAccess: IMissionDataAccess,
    override val thModeDataAccess: ITHModeDataAccess,
    private val gameConfigManager: GameConfigManager,
) : IDataAccessManager {
    
    override fun initialize() {
        // Load game config ở đây để các server service có thể sử dụng
        val hashGameConfig = libDataAccess.loadGameConfig()
        gameConfigManager.initialize(hashGameConfig)
    }
} 