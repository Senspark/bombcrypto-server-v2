package com.senspark.game.manager.blockReward

import com.senspark.common.service.IService
import com.senspark.common.service.Service
import com.senspark.game.data.model.user.RewardDetail
import com.senspark.game.data.model.user.UserBlockReward
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.smartfoxserver.v2.entities.data.ISFSArray

@Service("UserBlockRewardManager")
interface IUserBlockRewardManager : IService {
    fun toSfsArrays(): ISFSArray
    fun getRewardsMining(): MutableMap<BLOCK_REWARD_TYPE, RewardDetail>
    fun getRewardMining(blockRewardType: BLOCK_REWARD_TYPE): Float
    fun getRewardsMiningFi(): MutableMap<DataType, MutableMap<BLOCK_REWARD_TYPE,RewardDetail>>
    fun getRewardMiningFi(blockRewardType: BLOCK_REWARD_TYPE, dataType: DataType): Float
    fun addReward(rewardDetail: RewardDetail, dataType: DataType = DataType.UNKNOWN)
    fun onSaved()
    fun addRewards(rewards: Map<BLOCK_REWARD_TYPE, RewardDetail>)
    fun list(): List<UserBlockReward>
    fun get(blockRewardType: BLOCK_REWARD_TYPE): UserBlockReward?
    fun getByNetwork(blockRewardType: BLOCK_REWARD_TYPE): UserBlockReward?
    fun getTotalBcoinHaving(): Float
    fun loadUserBlockReward()
    fun getClaimFeeHaving(blockRewardType: BLOCK_REWARD_TYPE): Float
    fun getRewardValue(rewardType: BLOCK_REWARD_TYPE): Float
    fun getRewardValue(rewardType: BLOCK_REWARD_TYPE, dataType: DataType): Float
    fun getTotalGemHaving(): Float
    fun getTotalGoldHaving(): Float
    fun getTotalCoinHaving(): Float
    fun getTotalCoinFiHaving(dataType: DataType): Float
    fun getTotalRockHaving(): Float
    fun getTotalTonDepositHaving(): Float
    fun checkEnoughReward(value: Float, rewardType: BLOCK_REWARD_TYPE, isFiReward: Boolean = false)
    fun getTotalSolDepositHaving(): Float
    fun getTotalRonDepositHaving(): Float
    fun getTotalBasDepositHaving(): Float
    fun getTotalVicDepositHaving(): Float
    fun addMiningRewards(rewardDetail: RewardDetail)
    fun deductReward(value: Float, rewardType: BLOCK_REWARD_TYPE, dataType: DataType)
}