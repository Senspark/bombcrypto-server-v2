package com.senspark.game.manager

import com.senspark.common.utils.AppStage
import com.senspark.common.utils.RemoteLoggerInitData
import com.smartfoxserver.v2.extensions.ExtensionLogLevel
import java.time.Instant

class PropertyEnvManager : IEnvManager {
    override val appStage = AppStage.fromString(getEnv("APP_STAGE", "LOCAL"))
    override val isGke = getEnv("GKE", "0").toInt() == 1
    override val isGameServer = getEnv("IS_GAME_SERVER", "0").toInt() == 1
    override val isPvpServer = getEnv("IS_PVP_SERVER", "0").toInt() == 1
    override val isBnbServer = getEnv("IS_BNB_SERVER", "0").toInt() == 1
    override val isTonServer = getEnv("IS_TON_SERVER", "0").toInt() == 1
    override val isSolServer = getEnv("IS_SOL_SERVER", "0").toInt() == 1
    override val isRonServer = getEnv("IS_RON_SERVER", "0").toInt() == 1
    override val isBasServer = getEnv("IS_BAS_SERVER", "0").toInt() == 1
    override val isVicServer = getEnv("IS_VIC_SERVER", "0").toInt() == 1

    override val serverId = getEnv("SERVER_ID")
    override val messageSalt = getEnv("SALT")

    private val apBlockchainPath = getEnv("AP_BLOCKCHAIN")
    override val apLoginPath = getEnv("AP_LOGIN")
    override val apMarketPath = getEnv("AP_MARKET")
    override val apSignaturePath = getEnv("AP_SIGNATURE")
    private val apPvpMatchingPath = getEnv("AP_PVP_MATCHING")
    override val apMonetizationPath = getEnv("AP_MONETIZATION")
    override val apReferralPath = getEnv("AP_REFERRAL")

    override val logLevelDefault = getLogLevel(getEnv("LOG_LEVEL_DEFAULT", "DEBUG"))
    override val logDb = getEnv("LOG_DB", "false").toBoolean()
    override val logAll = getEnv("LOG_ALL", "false").toBoolean()
    override val logPvp = getEnv("LOG_PVP", "false").toBoolean()
    override val logHandler = getEnv("LOG_HANDLER", "false").toBoolean()
    override val logHttpRequest = getEnv("LOG_HTTP_REQUEST", "false").toBoolean()

    override val logRemoteData = RemoteLoggerInitData(
        serviceName = "sv-bomb",
        instanceId = serverId,
        stage = appStage,
        remoteHost = getEnv("LOG_REMOTE_HOST", "localhost:10002")
    )

    override val isTournamentGameServer = getEnv("IS_TOURNAMENT_GAME_SERVER", "false").toBoolean()

    override val syncHeroUrl = "${apBlockchainPath}/hero?address=%s&network=%s&mode=1"
    override val syncHouseUrl = "${apBlockchainPath}/house?address=%s&network=%s&mode=1"
    override val syncDepositedUrl = "${apBlockchainPath}/v3/deposited?address=%s&network=%s"
    override val getBomberStakeUrl = "${apBlockchainPath}/hero_stake_v2?id=%s&network=%s"
    override val checkValidCreateRockUrl = "${apBlockchainPath}/create_rock?network=%s"
    override val getPriceTokenUrl = "${apBlockchainPath}/coins_price"
    
    override val apSignatureToken = getEnv("AP_SIGNATURE_TOKEN")
    override val apLoginToken: String = getEnv("AP_LOGIN_TOKEN")
    override val mobileVerifyUrl = "${apLoginPath}/mobile/verify"
    override val mobileCreateAccount = "${apLoginPath}/mobile/create_senspark_account"
    override val tonVerifyLoginUrl = "${apLoginPath}/ton/verify"
    override val solVerifyLoginUrl = "${apLoginPath}/sol/verify"
    override val webVerifyLoginUrl = "${apLoginPath}/web/verify"
    override val bscVerifyLoginUrl = "${apLoginPath}/web/bsc/verify"
    override val polVerifyLoginUrl = "${apLoginPath}/web/pol/verify"
    override val ronVerifyLoginUrl = "${apLoginPath}/web/ron/verify"
    override val basVerifyLoginUrl = "${apLoginPath}/web/bas/verify"
    override val vicVerifyLoginUrl = "${apLoginPath}/web/vic/verify"
    
    override val apPvpMatchingConfigUrl: String = "${apPvpMatchingPath}/config?newServer=true"
    override val subscriptionPackageName = getEnv("SUBSCRIPTION_PACKAGE_NAME")
    override val pvpRankUpdateSeconds = getEnv("PVP_RANK_UPDATE_SECONDS", "3600").toInt()
    
    override val apSignatureCmdUpdateShieldUrl = "${apSignaturePath}/sign/upgrade-hero-shield?network=%s"
    override val apSignatureCmdCheckTotalClaimedUrl = "${apSignaturePath}/validate/check-total-claim-token?userAddress=%s&tokenType=%d&network=%s"
    override val apSignatureCmdClaimRewardUrl = "${apSignaturePath}/sign/claim-token?userAddress=%s&tokenType=%d&amount=%f&network=%s"
    
    override val avatarClubUrl = "https://game.bombcrypto.io/club_avatar/%s"

    override val addChildUrl = "${apReferralPath}/add-child?parent-id=%s&child-id=%s&identifier-name=%s"
    override val getMemberInfoUrl = "${apReferralPath}/get-member-info?id=%s"
    override val claimReferralUrl = "${apReferralPath}/claim?id=%s"
    override val addEarningUrl = "${apReferralPath}/add-earning?id=%s&addition=%s"

    override val redisConnectionString = getEnv("REDIS_CONNECTION_STRING")
    override val redisConsumerId = getEnv("SERVER_NAME", generateRandomString())

    override val postgresDriverName = "org.postgresql.Driver"
    override val postgresConnectionString = getEnv("POSTGRES_CONNECTION_STRING")
    override val postgresUsername = getEnv("POSTGRES_USERNAME")
    override val postgresPassword = getEnv("POSTGRES_PASSWORD")
    override val postgresMaxActiveConnections = getEnv("POSTGRES_MAX_ACTIVE_CONNECTIONS").toInt()
    override val postgresTestSql = "SELECT 1"

    override val schedulerThreadSize = getEnv("SCHEDULER_THREAD_SIZE", "20").toInt()

    override val serverStartTime = System.currentTimeMillis()

    override val useStreamListener = getEnv("USE_STREAM_LISTENER", "true").toBoolean()
    override val saveClientLogPath = getEnv("SAVE_CLIENT_LOG_PATH", "./clientLogs/")
    override val hashIdKey = getEnv("HASH_ID_KEY")

    override fun initialize() {
    }

    private fun getEnv(key: String, defaultValue: String? = null): String {
        return System.getenv(key)
            ?: defaultValue
            ?: throw IllegalArgumentException("Key not found: $key")
    }

    private fun getLogLevel(key: String): ExtensionLogLevel {
        return when (key) {
            "DEBUG" -> ExtensionLogLevel.DEBUG
            "INFO" -> ExtensionLogLevel.INFO
            "WARN" -> ExtensionLogLevel.WARN
            "ERROR" -> ExtensionLogLevel.ERROR
            else -> ExtensionLogLevel.DEBUG
        }
    }

    private fun generateRandomString(): String {
        return Instant.now().epochSecond.toString()
    }
}