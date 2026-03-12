package com.senspark.game.manager.stake

import com.senspark.common.service.IService
import com.senspark.common.service.Service
import com.senspark.game.declare.EnumConstants
import com.smartfoxserver.v2.entities.data.SFSArray

@Service("UserStakeVipManager")
interface UserStakeVipManager: IService {
    fun reload()
    fun toSfsArray(): SFSArray
    fun claim(rewardType: EnumConstants.StakeVipRewardType, type: String)
    fun claimRemainingReward()
    fun isVip(): Boolean
}