package com.senspark.lib.data.manager

import com.senspark.common.service.IGlobalService

interface IGameConfigManager : IGlobalService {
    val maintenanceTimestamp: Long
    val isKickWhenHack: Int
    val timeBombExplode: Int
    val nextTimeCanClaimReward: Int
    val minPve2Reward: Float
    val maxPve2Reward: Float
    val countDownCheckUserData: Int
    val versionCode: Int
    val minVersionCanPlay: Int
    val claimBcoinLimit: List<List<Int>>
    val feeSell: Float
    val newUserGiftSkin: List<Int>
    val sizeRankingLeaderboard: Int
    val chanelSlackId: String
    val tokenBotSlack: String
    val isUseExplodeV2Handler: Boolean
    val isUseExplodeV3Handler: Boolean
    val minStakeBcoinTHV1: List<Int>
    val pvpRankingSeasonDay: Int
    val minStakeSenTHV1: List<Int>
    val isServerGameTest: Boolean
    val minPvpMatchCountToGetReward: Int
    val serverMaintenance: Int
    val tonServerMaintenance: Int
    val maxConcurrentLogin: Int
    val maxBomberActive: Int
    val blockDensity: Float
    val maxTitleset: Int
    val energyMultiplyByStamina: Int
    val openSkinChestCost: Float
    val pvpRewardFee: Float
    val pvpTicketPrice: Int
    val enableClaimToken: Boolean
    val enableClaimTokenDeposited: Boolean
    val enableClaimHero: Boolean
    val newUserTonGiftHero: List<Int>
    val newUserAirdropGiftHero: List<Int>
    val countDownSaveUserData: Int
    val enableGetServerInfoTon: Boolean
    val enableGetServerInfoWeb: Boolean
    val urlConfigTasks: String
    val minClaimReferral: Int
    val timePayOutReferral: Int
    val enableClaimReferral: Boolean
    val iosHeroLoaded: Int
    val bidUnitPrice: Float
    val heroSpecialColor: Int
    val dailyTaskConfigUrl: String
    val totalTaskInDay: Int
    val refreshMinPriceMarket: Int
    val refreshMinPriceClient: Int
    val coinRankingSeasonDay: Int

    // ----------------- Custom -----------------
    fun getString(key: String, default: String = ""): String
    fun getInt(key: String, default: Int = 0): Int
    fun getFloat(key: String, default: Float = 0f): Float
    fun getLong(key: String, default: Long = 0L): Long
    fun initialize(hash: Map<String, String>)
}