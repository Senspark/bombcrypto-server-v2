package com.senspark.game.service

import com.senspark.common.data.IBombRank
import com.senspark.common.data.LogPlayPvPData
import com.senspark.common.pvp.IRankManager
import com.senspark.common.service.IGlobalService
import com.senspark.common.service.IService
import com.senspark.common.service.Service
import com.senspark.game.constant.EventName
import com.senspark.game.constant.ItemType
import com.senspark.game.data.*
import com.senspark.game.data.model.user.PvPRank
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.pvp.entity.BlockType

interface IPvpDataAccess : IService, IGlobalService {
    fun queryEnableLogPvPCommands(): Set<String>
    fun queryEvent(): Map<EventName, EventData>
    fun queryLogPlayPvP(walletAddress: String): List<LogPlayPvPData>
    fun queryRankingWithPoint(): List<IBombRank>
    fun queryRankingWithPoint(currentPoint: Int): IBombRank
    fun queryVipBonusPvPRankingPoint(): List<VipWinBonusPvPRankingPointData>
    fun queryPvPBlockHealth(): List<PvPBlockHealthData>
    fun queryPvPBonusPoint(): PvPBonusPointData
    fun queryPvPChestDensity(): Float
    fun queryPvPChestDropRate(): Map<BlockType, Float>
    fun queryPvPItemDropRate(): Map<BlockType, Float>
    fun queryPvPMatchingPointDelta(): List<PvPMatchingPointDeltaData>
    fun queryPvPRank(season: Int, rankManager: IRankManager): MutableMap<Int, PvPRank>
    fun queryWinBonusPvPRankingPoint(default: Int, season: Int, userId: Int, dataType: DataType): Int
    fun queryPvPChestSpawnRadius(): Pair<Int, Int>
    fun querySkinChestDropRate(): List<SkinChestDropRateData>
    fun update(action: () -> Unit)
    fun updateData(endTime: String, startTime: String)
    fun updateSkinChest(cost: Float, skinChest: OpenSkinChestData, userId: Int, userName: String)
    fun updateSkinChest(id: Int, userId: Int, status: Int)
    fun updateSkinChest(userId: Int, itemType: ItemType, activeSkinIds: List<Int>)
    fun updateUserReward(userId: Int, rewards: List<RewardData>, reason: String)
    fun updateUserRank(userId: Int, isWinner: Boolean, deltaPoint: Int, season: Int)
    fun decayUserRank(season: Int, decayUsers: MutableMap<Int, Int>)
    fun getAmountPvpMatchesCurrentDate(userId: Int, season: Int): Int
    fun getAllAmountPvpMatchesCurrentDate(season: Int): Map<Int, Int>
    fun getTotalPvpMatches(userId: Int): Int
}