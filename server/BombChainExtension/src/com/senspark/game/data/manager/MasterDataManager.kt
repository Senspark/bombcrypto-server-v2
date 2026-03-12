package com.senspark.game.data.manager

import com.senspark.common.pvp.IRankManager
import com.senspark.game.constant.Booster
import com.senspark.game.constant.GameMode
import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.manager.season.IPvpSeasonManager
import com.senspark.game.data.model.config.MarketItemConfig
import com.senspark.game.db.IGameFeatureConfigManager
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.user.IUserInventoryManager
import com.senspark.lib.data.manager.GameConfigManager
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class MasterDataManager(
    private val configItemManager: IConfigItemManager,
    private val configHeroTraditionalManager: IConfigHeroTraditionalManager,
    private val gameFeatureConfigManager: IGameFeatureConfigManager,
    private val pvpRankManager: IRankManager,
    private val pvpSeasonManager: IPvpSeasonManager,
    private val userDataAccess: IUserDataAccess,
    private val gameConfigManager: IGameConfigManager,
) : IMasterDataManager {

    companion object {
        private const val STATUS_NORMAL = 0
        private const val STATUS_RECOMMEND_UPDATE = 1
        private const val STATUS_FORCE_UPDATE = 2
    }
    private lateinit var _onBoardConfig: Map<Int, Float>

    override fun initialize() {
        _onBoardConfig = userDataAccess.getOnBoardingConfig()
    }
    
    /**
     * FIXME: bad optimization
     */
    override fun getGameConfig(clientBuildVersion: Int): ISFSObject {
        return SFSObject().apply {
            putSFSArray("config_hero_traditional", configHeroTraditionalManager.toSFSArray())
            putSFSArray("product_items", configItemManager.toSFSArray())
            putSFSArray("bomb_rank_config", pvpRankManager.toSFSArray())
            putInt("update_status", getUpdateStatusOnClientBuildVersion(clientBuildVersion))
            putLong("skin_item_expiry_time", IUserInventoryManager.DEFAULT_SKIN_ITEM_EXPIRY_TIME_IN_MILLIS)
            putIntArray(
                "disable_features",
                gameFeatureConfigManager.getDisableFeaturesByVersion(clientBuildVersion)
            )
            putIntArray(
                "item_id_booster_pvp",
                Booster.entries.filter { it.gameMode == GameMode.ALL || it.gameMode == GameMode.PVP }
                    .map { it.value })
            putIntArray(
                "item_id_booster_adv",
                Booster.entries.filter { it.gameMode == GameMode.ALL || it.gameMode == GameMode.ADV }
                    .map { it.value })
            putInt("current_season", pvpSeasonManager.currentSeasonNumber)
        }
    }

    override fun getOnBoardingConfig(): Map<Int, Float> {
        return _onBoardConfig
    }



    private fun getUpdateStatusOnClientBuildVersion(clientBuildVersion: Int): Int {
        val currentVersion = gameConfigManager.versionCode
        val minVersionCanPlay = gameConfigManager.minVersionCanPlay
        return when {
            clientBuildVersion < minVersionCanPlay -> STATUS_FORCE_UPDATE
            clientBuildVersion < currentVersion -> STATUS_RECOMMEND_UPDATE
            else -> STATUS_NORMAL
        }
    }
}