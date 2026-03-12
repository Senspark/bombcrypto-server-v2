package com.senspark.lib.data.manager

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.senspark.common.utils.ILogger
import java.lang.reflect.Type
import java.time.Instant

class GameConfigManager(logger: ILogger) : BaseDataManager<String, String>(logger), IGameConfigManager {
    private var _claimBcoinLimit: List<List<Int>> = ArrayList()
    private var _newUserGiftSkin: List<Int> = ArrayList()
    private var _minStakeBcoinTHV1: List<Int> = ArrayList()
    private var _minStakeSenTHV1: List<Int> = ArrayList()
    private var _newUserTonGiftHero: List<Int> = ArrayList()
    private var _newUserAirdropGiftHero: List<Int> = ArrayList()

    override fun initialize(hash: Map<String, String>) {
        super.initialize(hash)
        try {
            val gson = Gson()

            val claimBcoinLimitString = getString("claim_bcoin_limit")
            _claimBcoinLimit = gson.fromJson(claimBcoinLimitString, object : TypeToken<List<List<Int>>>() {}.type)

            _newUserGiftSkin = gson.fromJson(getString("new_user_gift_skin"), object : TypeToken<List<Int>>() {}.type)

            _minStakeBcoinTHV1 =
                gson.fromJson(getString("min_stake_bcoin_th_v1"), object : TypeToken<List<Int>>() {}.type)

            _minStakeSenTHV1 =
                gson.fromJson(getString("min_stake_sen_th_v1"), object : TypeToken<List<Int>>() {}.type)

            _newUserTonGiftHero =
                gson.fromJson(getString("new_user_ton_gift_hero"), object : TypeToken<List<Int>>() {}.type)

            _newUserAirdropGiftHero =
                gson.fromJson(getString("new_user_ton_gift_hero"), object : TypeToken<List<Int>>() {}.type)

        } catch (e: Exception) {
            logger.error(e)
        }
    }

    override val maintenanceTimestamp get() = Instant.parse(getString("maintenance_timestamp", "")).epochSecond
    override val isKickWhenHack get() = getInt("is_kick_when_hack", 0)
    override val timeBombExplode get() = getInt("time_bomb_explode", 3000)
    override val nextTimeCanClaimReward get() = getInt("next_time_can_claim_reward", 1440) // 1 day = 1440 minutes
    override val minPve2Reward get() = getFloat("min_pve_v2_reward", 2.5f)
    override val maxPve2Reward get() = getFloat("max_pve_v2_reward", 4f)
    override val countDownCheckUserData get() = getInt("count_down_check_user_data", 3600) // 1 hour = 3600 seconds
    override val versionCode get() = getInt("version_code", 1)
    override val minVersionCanPlay get() = getInt("min_version_can_play", 1)
    override val claimBcoinLimit get() = _claimBcoinLimit
    override val feeSell get() = getFloat("fee_sell", 0.2f)
    override val newUserGiftSkin get() = _newUserGiftSkin
    override val sizeRankingLeaderboard get() = getInt("size_ranking_leaderboard", 100)
    override val chanelSlackId get() = getString("chanel_slack_id", "")
    override val tokenBotSlack get() = getString("token_bot_slack", "")
    override val isUseExplodeV2Handler get() = getInt("is_explode_v2_handler", 1) == 1
    override val isUseExplodeV3Handler get() = getInt("is_explode_v3_handler", 1) == 1
    override val minStakeBcoinTHV1 get() = _minStakeBcoinTHV1
    override val pvpRankingSeasonDay get() = getInt("pvp_ranking_season_day", 27)
    override val minStakeSenTHV1 get() = _minStakeSenTHV1
    override val isServerGameTest get() = getInt("is_server_game_test", 0) == 1
    override val minPvpMatchCountToGetReward get() = getInt("pvp_match_reward", 30)
    override val serverMaintenance get() = getInt("is_server_maintenance", 0)
    override val tonServerMaintenance get() = getInt("is_ton_server_maintenance", 0)
    override val maxConcurrentLogin get() = getInt("max_concurrent_login", 15)
    override val maxBomberActive get() = getInt("max_bomber_active", 15)
    override val blockDensity get() = getFloat("block_density", 0.7f)
    override val maxTitleset get() = getInt("max_title_set", 2)
    val timeStartServer get() = getString("time_start_server", "2021-09-01 00:00:01")
    override val energyMultiplyByStamina get() = getInt("energy_multiply_by_stamina", 50)
    override val openSkinChestCost get() = getFloat("shard_pvp_number", 5f)
    override val pvpRewardFee get() = getFloat("pvp_reward_fee", 0.9f)
    override val pvpTicketPrice get() = getInt("pvp_ticket_price", 20)
    override val enableClaimToken get() = getInt("enable_claim_token", 0) == 1
    override val enableClaimTokenDeposited get() = getInt("enable_claim_token_deposited", 0) == 1
    override val enableClaimHero get() = getInt("enable_claim_hero", 0) == 1
    override val newUserTonGiftHero get() = _newUserTonGiftHero
    override val newUserAirdropGiftHero get() = _newUserAirdropGiftHero
    override val countDownSaveUserData get() = getInt("count_down_save_user_data", 300) // 5 minutes = 300 seconds
    override val enableGetServerInfoTon get() = getInt("enable_get_server_info_ton", 0) == 1
    override val enableGetServerInfoWeb get() = getInt("enable_get_server_info_web", 0) == 1
    override val urlConfigTasks get() = getString("url_config_tasks", "")
    override val minClaimReferral get() = getInt("min_claim_referral", 50)
    override val timePayOutReferral get() = getInt("time_pay_out_referral", 24)
    override val enableClaimReferral get() = getInt("enable_claim_referral", 0) == 1
    override val iosHeroLoaded get() = getInt("ios_hero_loaded", 100)
    override val bidUnitPrice get() = getFloat("bid_unit_price", 0.4f)
    override val heroSpecialColor get() = getInt("hero_special_color", 6)
    override val dailyTaskConfigUrl get() = getString("daily_task_config_url", "https://game.bombcrypto.io/daily_task/data/data_v1.json")
    override val totalTaskInDay get() = getInt("total_task_in_day", 5)
    override val refreshMinPriceMarket get() = getFloat("refresh_min_price_market", 30f).toInt() // 30 seconds
    override val refreshMinPriceClient get() = getFloat("refresh_min_price_client", 60f).toInt() // 60 seconds
    override val coinRankingSeasonDay get() = getInt("coin_ranking_season_day", 27) // Default is day 27

    // ----------------- Custom -----------------
    override fun getString(key: String, default: String): String {
        return get(key) ?: default
    }

    override fun getInt(key: String, default: Int): Int {
        return hashData[key]?.toIntOrNull() ?: default
    }

    override fun getFloat(key: String, default: Float): Float {
        return hashData[key]?.toFloatOrNull() ?: default
    }

    override fun getLong(key: String, default: Long): Long {
        return hashData[key]?.toLongOrNull() ?: default
    }

    override fun initialize() {
    }
}
