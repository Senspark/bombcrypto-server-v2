package com.senspark.game.db

import com.senspark.common.IDatabase
import com.senspark.common.service.IGlobalService
import com.senspark.game.db.dailyMission.IMissionDataAccess
import com.senspark.game.db.gachaChest.IGachaChestDataAccess
import com.senspark.game.service.IPvpDataAccess
import com.senspark.lib.db.ILibDataAccess

interface IDataAccessManager : IGlobalService {
    val database: IDatabase
    val shopDataAccess: IShopDataAccess
    val gameDataAccess: IGameDataAccess
    val logDataAccess: ILogDataAccess
    val userDataAccess: IUserDataAccess
    val rewardDataAccess: IRewardDataAccess
    val libDataAccess: ILibDataAccess
    val pvpDataAccess: IPvpDataAccess
    val gachaChestDataAccess: IGachaChestDataAccess
    val missionDataAccess: IMissionDataAccess
    val iapDataAccess: IIapDataAccess
    val thModeDataAccess: ITHModeDataAccess
    val tournamentDataAccess: IPvpTournamentDataAccess
}