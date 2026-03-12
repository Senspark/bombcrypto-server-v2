package com.senspark.game.constant

import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE.GEM
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE.GOLD

enum class ItemPackage(val expirationAfter: Long? = null, val rewardType: BLOCK_REWARD_TYPE) {
    GOLD_7(604800000, GOLD),
    GEM_7(604800000, GEM),
    GEM_30(2592000000, GEM),
    BUY_GOLD(rewardType = GOLD),
    BUY_GEM(rewardType = GEM);
}