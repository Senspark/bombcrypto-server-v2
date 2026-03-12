package com.senspark.game.data.model.config

import com.senspark.common.utils.toSFSArray
import com.senspark.game.constant.ItemType
import com.senspark.game.declare.customEnum.MissionRewardType
import com.senspark.game.exception.CustomException
import com.senspark.lib.utils.Util
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.Serializable

@Serializable
class MissionReward(
    val type: MissionRewardType,
    val itemType: ItemType,
    val rewards: List<MissionRewardItem>,
    val quantity: Int = 0,
    val isLock: Boolean = false,
    val expirationAfter: Long = 0

) {

    fun getRandomReward(): MissionRewardItem? {
        if (type != MissionRewardType.RANDOM_IN_LIST) {
            throw CustomException("Type ${type.name} cannot ${MissionRewardType.RANDOM_IN_LIST.name}")
        }
        return if (rewards.isEmpty()) {
            null
        } else {
            rewards[Util.randIndex(rewards.size)]
        }
    }

    fun toSfsObject(): SFSObject {
        return SFSObject.newInstance().apply {
            putUtfString("type", type.name)
            putUtfString("item_type", itemType.name)
            putInt("quantity", quantity)
            putBool("is_lock", isLock)
            putLong("expiration_after", expirationAfter)
            putSFSArray("rewards", rewards.toSFSArray { it.toSfsObject() })
        }
    }
}


@Serializable
class MissionRewardItem(
    val itemId: Int = 0,
    val quantity: Int = 1
) {
    fun toSfsObject(): SFSObject? {
        return SFSObject.newInstance().apply {
            putInt("item_id", itemId)
            putInt("quantity", quantity)
        }
    }
}