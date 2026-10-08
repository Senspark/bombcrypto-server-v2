package com.senspark.game.data.manager

import com.senspark.game.user.IUserInventoryManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class NullMasterDataManager : IMasterDataManager {

    override fun initialize() {
    }

    // Phải trả đủ key như MasterDataManager: client đọc thẳng từng key, thiếu key thì
    // GetSFSArray() trả null và crash lúc parse response.
    override fun getGameConfig(clientBuildVersion: Int): ISFSObject {
        return SFSObject().apply {
            putSFSArray("config_hero_traditional", SFSArray())
            putSFSArray("product_items", SFSArray())
            putSFSArray("bomb_rank_config", SFSArray())
            putInt("update_status", 0)
            putLong("skin_item_expiry_time", IUserInventoryManager.DEFAULT_SKIN_ITEM_EXPIRY_TIME_IN_MILLIS)
            putIntArray("disable_features", emptyList())
            putIntArray("item_id_booster_pvp", emptyList())
            putIntArray("item_id_booster_adv", emptyList())
            putInt("current_season", 0)
        }
    }

    override fun getOnBoardingConfig(): Map<Int, Float> {
        return emptyMap()
    }

}
