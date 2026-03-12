package com.senspark.game.db

import com.senspark.common.service.IGlobalService
import com.senspark.game.data.model.config.*
import com.senspark.game.data.model.user.CoinRank
import com.senspark.game.data.model.user.ClubInfo
import com.senspark.game.data.model.user.UserClub
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.manager.treasureHuntV2.MultipleRewardResult
import com.senspark.game.manager.treasureHuntV2.UserId
import java.time.Instant

interface ITHModeDataAccess : IGlobalService {
    fun getNextRaceId(): Int
    fun loadTHModeV2Config(): Map<BLOCK_REWARD_TYPE, TreasureHuntV2Config>
    fun loadRewardPoolConfig(): Map<BLOCK_REWARD_TYPE, MutableMap<Int, Double>>
    fun updateTHModeV2Pool(newRewardPool: MutableMap<Int, Double>, type: BLOCK_REWARD_TYPE)
    fun refillTHModeV2Pool()
    fun loadMaxTHModeV2Pool(): Map<BLOCK_REWARD_TYPE, Map<Int, Double>>
    fun writeLogTHModeRewards(raceId: Int, data: Map<UserId, List<MultipleRewardResult>>)
    fun loadRewardLevelConfig(): Map<Int, RewardLevelConfig>
    fun loadTreasureHuntDataConfig(): TreasureHuntDataConfig
    fun saveRankingCoin(uid: Int, coin: Float, network: DataType, currentSeason: Int)
    fun getRankingCoin(currentSeason: Int, dataType: DataType): List<CoinRank>
    fun addNewAirdropSeason(newSeasonId: Int, timeStartNextSeason: Instant, timeEndNextSeason: Instant)
    fun logUserAirdropBuyActivity(uid: Int, itemIds: String, price: Float, type: String, dataType: DataType)
    fun logOfflineReward(uid: Int, timeLogOut: Instant, timeOffline: Double, reward: Double, dataType: DataType)
    fun getTasksConfig(): MutableMap<Int, TonTasksConfig>
    fun saveTaskComplete(uid: Int, taskId: Int)
    fun chaimTask(uid: Int, taskId: Int, reward: Double)
    fun getUserCompletedTasks(uid: Int): MutableMap<Int, Boolean>
    fun logUserFusionHeroServer(
        uid: Int,
        heroIds: List<Int>,
        result: String,
        reasonFail: String,
        amountTonNeedToFusion: Double,
        dataType: DataType
    )

    fun logClaimReferral(uid: Int, amount: Float)
    fun getReferralParamsConfig(): MutableMap<String, Int>
    fun getCoinLeaderboardConfig(): List<CoinLeaderboardConfig>
    fun getAllClub(season: Int): MutableList<ClubInfo>
    fun addNewClub(idTelegram: Long, name: String, link: String, season: Int, avatarName: String): Int
    fun addNewClubV2(name: String, season: Int, type: EnumConstants.ClubType) : Int
    fun getUserClubs(season: Int): MutableMap<Int, UserClub>
    fun joinClub(uid: Int, clubId: Int, season: Int)
    fun leaveClub(uid: Int)
    fun addClubPoint(clubId: Int, season: Int, point: Double)
    fun addMemberClubPoint(uid: Int, clubId: Int, season: Int, point: Double)
    fun summaryClubPoint(season: Int)
    fun getBidPrice(): Map<Int, Int>
    fun getTopClubBid(): MutableMap<Int, Int>
    fun getCurrentClubBid(): MutableMap<Int, Int>
    fun addClubBidPoint(clubId: Int, bidPoint: Int)
}
