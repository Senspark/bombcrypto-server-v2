package com.senspark.game.manager.ads

import com.senspark.common.utils.toSFSArray
import com.senspark.game.api.IVerifyAdApiManager
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.RewardData
import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.data.manager.luckyWheel.ILuckyWheelRewardManager
import com.senspark.game.data.model.user.AddUserItemWrapper
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DeviceType
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.customEnum.ChangeRewardReason
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.senspark.game.service.IPvpDataAccess
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class UserBonusRewardManager(
    private val _mediator: UserControllerMediator,
    private val _verifyAdApi: IVerifyAdApiManager,
    private val _userBlockRewardManager: IUserBlockRewardManager,
    private val saveGameAndLoadReward: () -> Unit
) : IUserBonusRewardManager {

    private val _dataAccessManager = _mediator.services.get<IDataAccessManager>()
    private val _pvpDataAccess: IPvpDataAccess = _dataAccessManager.pvpDataAccess
    private val _rewardDataAccess: IRewardDataAccess = _dataAccessManager.rewardDataAccess

    private val _luckyWheelRewardManager = _mediator.svServices.get<ILuckyWheelRewardManager>()
    private val _configHeroTraditionalManager = _mediator.svServices.get<IConfigHeroTraditionalManager>()

    private val rewardsAds: MutableMap<String, MutableList<RewardData>> = mutableMapOf()
    private val syncReward = Any()

    companion object {
        const val LUCKY_WHEEL_GOLD_PRICE = 100f
    }

    override fun addRewardsAds(rewardId: String, reward: RewardData?) {
        synchronized(syncReward) {
            rewardsAds.getOrPut(rewardId) { mutableListOf() }.apply {
                reward?.let { add(it) }
            }
        }
    }

    private fun removeRewardsAds(rewardId: String) {
        rewardsAds.remove(rewardId)
    }

    override suspend fun takeBonusReward(rewardId: String, adsToken: String): List<RewardData> {
        val isValidAds = _verifyAdApi.isValidAds(adsToken)
        synchronized(syncReward) {
            if (!isValidAds) {
                removeRewardsAds(rewardId)
                throw CustomException("Ads invalid", ErrorCode.INVALID_PARAMETER)
            }
            val rewards = rewardsAds[rewardId] ?: throw CustomException("Reward not exists")
            _pvpDataAccess.updateUserReward(_mediator.userId, rewards, ChangeRewardReason.BONUS_REWARD_PVP)
            return rewards
        }
    }

    override suspend fun takeLuckyWheelReward(
        rewardId: String,
        adsToken: String,
        deviceType: DeviceType
    ): ISFSObject {
        var isValidAds = true
        if(deviceType == DeviceType.MOBILE) {
            isValidAds = _verifyAdApi.isValidAds(adsToken)
        }
        synchronized(syncReward) {
            if (!rewardsAds.containsKey(rewardId)) {
                // Chưa rõ lý do đôi lúc ko rewardId ko đc set vào đây trước khi user gọi lucky wheel nên ko cần check nữa
                // vì đã check ads id với reward rồi, reward cũng đc random phía sau nên chỗ này ko có ý nghĩa lắm
                //throw CustomException("Reward not exists")
            }
            val rewardSpent = mutableMapOf<BLOCK_REWARD_TYPE, Float>()
            when (deviceType) {
                // case web user 100 gold
                DeviceType.WEB -> {
                    _userBlockRewardManager.checkEnoughReward(
                        LUCKY_WHEEL_GOLD_PRICE,
                        BLOCK_REWARD_TYPE.GOLD
                    )
                    rewardSpent[BLOCK_REWARD_TYPE.GOLD] = LUCKY_WHEEL_GOLD_PRICE
                }
                //case mobile verify ads
                DeviceType.MOBILE -> {
                    if (!isValidAds) {
                        removeRewardsAds(rewardId)
                        throw CustomException("Ads invalid", ErrorCode.INVALID_PARAMETER)
                    }
                }

                else -> throw CustomException("Device type invalid", ErrorCode.INVALID_PARAMETER)
            }

            val reward = _luckyWheelRewardManager.randomReward()
            val rewardsReceive = mutableListOf<AddUserItemWrapper>()
            reward.second.forEach {
                rewardsReceive.add(
                    AddUserItemWrapper(
                        it.first,
                        it.second,
                        expirationAfter = it.third,
                        userId = _mediator.userId,
                        configHeroTraditional = _configHeroTraditionalManager
                    )
                )
            }
            _rewardDataAccess.addTRRewardForUser(
                _mediator.userId,
                _mediator.dataType,
                rewardsReceive,
                saveGameAndLoadReward,
                "Lucky_wheel",
                rewardSpent = rewardSpent
            )
            removeRewardsAds(rewardId)
            return SFSObject().apply {
                putUtfString("reward_code", reward.first.code)
                putSFSArray("items", reward.second.toSFSArray {
                    SFSObject().apply {
                        putInt("item_id", it.first.id)
                        putInt("quantity", it.second)
                    }
                })
            }
        }
    }
}
