package com.senspark.game.manager

import com.senspark.common.service.IGlobalService
import com.senspark.common.utils.AppStage
import com.senspark.common.utils.RemoteLoggerInitData
import com.smartfoxserver.v2.extensions.ExtensionLogLevel

interface IEnvManager : IGlobalService {
    val isGke: Boolean
    val appStage: AppStage
    val isGameServer: Boolean
    val isBnbServer: Boolean
    val isPvpServer: Boolean
    val isTonServer: Boolean
    val isSolServer: Boolean
    val isRonServer: Boolean
    val isBasServer: Boolean
    val isVicServer: Boolean

    val logLevelDefault: ExtensionLogLevel
    val logAll: Boolean
    val logPvp: Boolean
    val logDb: Boolean
    val logHandler: Boolean
    val logHttpRequest: Boolean
    val logRemoteData: RemoteLoggerInitData
    
    val isTournamentGameServer: Boolean

    val serverId: String
    val messageSalt: String

    val apLoginPath: String
    val apMonetizationPath: String
    val syncHeroUrl: String
    val syncHouseUrl: String
    val syncDepositedUrl: String
    val tonVerifyLoginUrl: String
    val solVerifyLoginUrl: String
    val webVerifyLoginUrl: String
    val bscVerifyLoginUrl: String
    val polVerifyLoginUrl: String
    val ronVerifyLoginUrl: String
    val basVerifyLoginUrl: String
    val vicVerifyLoginUrl: String
    
    val apMarketPath: String

    val apSignatureToken: String
    val apSignaturePath: String
    val apLoginToken: String
    val apPvpMatchingConfigUrl: String
    val getBomberStakeUrl: String
    val checkValidCreateRockUrl: String
    val apSignatureCmdUpdateShieldUrl: String
    val getPriceTokenUrl: String
    val avatarClubUrl: String

    //referral 
    val apReferralPath: String
    val addChildUrl: String
    val getMemberInfoUrl: String
    val claimReferralUrl: String
    val addEarningUrl: String

    //claim signature
    val apSignatureCmdCheckTotalClaimedUrl: String
    val apSignatureCmdClaimRewardUrl: String

    val mobileVerifyUrl: String
    val mobileCreateAccount: String
    val subscriptionPackageName: String

    /**
     * Thời gian schedule cập nhật lại Rank Pvp Leaderboard
     */
    val pvpRankUpdateSeconds: Int

    val redisConnectionString: String
    val redisConsumerId: String

    val postgresDriverName: String
    val postgresConnectionString: String
    val postgresUsername: String
    val postgresPassword: String
    val postgresMaxActiveConnections: Int
    val postgresTestSql: String

    val schedulerThreadSize: Int

    val serverStartTime: Long

    /** Set false để không lắng nghe Redis Stream (tránh trường hợp nhiều server cùng xử lý Stream dẫn đến double transaction) */
    val useStreamListener: Boolean
    val saveClientLogPath: String

    val hashIdKey: String
}