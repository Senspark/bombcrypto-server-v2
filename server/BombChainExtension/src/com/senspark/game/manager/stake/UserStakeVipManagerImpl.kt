package com.senspark.game.manager.stake

import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.stake.IStakeVipRewardManager
import com.senspark.game.data.model.user.UserStakeVipReward
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.declare.EnumConstants.StakeVipRewardType
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class UserStakeVipManagerImpl(
    private val _mediator: UserControllerMediator,
) : UserStakeVipManager {
    private val dataAccessManager = _mediator.services.get<IDataAccessManager>()
    private val stakeVipRewardManager = _mediator.svServices.get<IStakeVipRewardManager>()
    
    private lateinit var rewards: List<UserStakeVipReward>

    override fun destroy() = Unit

    override fun isVip(): Boolean {
        if (!this::rewards.isInitialized) {
            this.load()
        }
        return rewards.isNotEmpty()
    }

    private fun load() {
        rewards = dataAccessManager.userDataAccess.loadUserStakeVip(_mediator.userName)
    }

    override fun toSfsArray(): SFSArray {
        if (!this::rewards.isInitialized) {
            this.load()
        }

        val sfsArray = SFSArray()
        stakeVipRewardManager.rewards.forEach {
            val rewards = SFSArray()
            it.value.forEach { it2 ->
                val userReward = this.rewards.find { re ->
                    re.level == it.key && re.type == it2.type && it2.rewardType == re.rewardType
                }
                val sfsObject = it2.toSfsObject()
                userReward?.updateQuantityIfEnoughClaimTime()
                sfsObject.putInt("havingQuantity", userReward?.havingQuantity ?: 0)
                sfsObject.putLong("nextClaim", userReward?.nextClaim ?: 0)
                rewards.addSFSObject(sfsObject)
            }
            val item = SFSObject()
            item.putInt("level", it.key)
            item.putBool("currentVip", this.rewards.any { it2 -> it2.level == it.key })
            item.putDouble("stake_amount", if (it.value.isEmpty()) 0.0 else it.value[0].stakeAmount)
            item.putSFSArray("reward", rewards)
            sfsArray.addSFSObject(item)
        }
        return sfsArray
    }

    override fun claim(rewardType: StakeVipRewardType, type: String) {
        if (!this::rewards.isInitialized) {
            this.load()
        }
        val reward = rewards.find { it.rewardType == rewardType && it.type == type }
            ?: throw CustomException("Reward not exists", ErrorCode.SERVER_ERROR)
        if (reward.canClaim()) {
            dataAccessManager.userDataAccess.claimStakeVipReward(_mediator.userId, _mediator.dataType, reward)
        }

//        reload lại
        this.load()
    }

    override fun claimRemainingReward() {
        if (!this::rewards.isInitialized) {
            this.load()
        }
        rewards.forEach {
            if (it.canClaim(false)) {
                dataAccessManager.userDataAccess.claimStakeVipReward(_mediator.userId, _mediator.dataType, it)
            }
        }
    }

    override fun reload() {
        this.load()
    }
}