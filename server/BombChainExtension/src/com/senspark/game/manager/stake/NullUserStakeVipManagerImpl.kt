package com.senspark.game.manager.stake

import com.senspark.game.declare.EnumConstants.StakeVipRewardType
import com.smartfoxserver.v2.entities.data.SFSArray

class NullUserStakeVipManagerImpl : UserStakeVipManager {
    override fun destroy() = Unit

    override fun isVip(): Boolean {
        return true
    }

    override fun toSfsArray(): SFSArray {
        return SFSArray()
    }

    override fun claim(rewardType: StakeVipRewardType, type: String) {}

    override fun claimRemainingReward() {}

    override fun reload() {}
}