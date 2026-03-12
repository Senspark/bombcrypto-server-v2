package com.senspark.game.data.model.user

import com.senspark.game.constant.ItemType
import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.data.model.config.Item
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.exception.CustomException
import com.smartfoxserver.v2.entities.data.SFSObject

class AddUserItemWrapper(
    val item: Item,
    val quantity: Int,
    val isLock: Boolean = true,
    val expirationAfter: Long = 0,
    userId: Int? = null,
    configHeroTraditional: IConfigHeroTraditionalManager? = null,
    val isEquip: Boolean = false,
) {
    val hero: Hero? = if (item.type == ItemType.HERO) {
        if (configHeroTraditional == null) {
            throw CustomException("configHeroTraditional must not be null ")
        }
        if (userId == null) {
            throw CustomException("userId must not be null")
        }
        configHeroTraditional.createHero(item.id, userId)
    } else null

    fun toSfsObject(): SFSObject {
        return SFSObject().apply {
            putInt("item_id", item.id)
            putUtfString("item_type", item.type.name)
            putInt("quantity", quantity)
            putBool("is_lock", isLock)
            putLong("expiration_after", expirationAfter)
        }
    }
}