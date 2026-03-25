package com.senspark.game.extension.events

import com.senspark.common.utils.IGlobalLogger
import com.senspark.common.utils.IServerLogger
import com.senspark.game.api.ApiException
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.declare.EnumConstants.*
import com.senspark.game.declare.SFSField
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.extension.modules.ServerType
import com.senspark.game.handler.MainGameExtensionBaseEventHandler
import com.senspark.game.manager.online.IUserOnlineManager
import com.senspark.game.manager.ton.IForceLoginManager
import com.senspark.game.utils.ServerError
import com.senspark.game.utils.tryGetField
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.bitswarm.sessions.ISession
import com.smartfoxserver.v2.core.ISFSEvent
import com.smartfoxserver.v2.core.SFSEventParam
import com.smartfoxserver.v2.entities.data.SFSObject
import com.smartfoxserver.v2.exceptions.SFSErrorData
import com.smartfoxserver.v2.exceptions.SFSLoginException
import com.senspark.common.cache.ICacheService
import com.senspark.game.declare.UserNameSuffix
import com.senspark.game.constant.CachedKeys
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class UserLoginHandler : MainGameExtensionBaseEventHandler() {
    private val _config = globalServices.get<IGameConfigManager>()
    private val _logger = globalServices.get<IGlobalLogger>()
    private val _userOnlineManager = globalServices.get<IUserOnlineManager>()

    override fun handleServerEvent(event: ISFSEvent) {
        var identity = ""
        try {
            val session = event.getParameter(SFSEventParam.SESSION) as ISession
            val userNameFromClient = event.getParameter(SFSEventParam.LOGIN_NAME) as String
            
            identity = userNameFromClient
            checkServerMaintenance(userNameFromClient)

            val data = event.getParameter(SFSEventParam.LOGIN_IN_DATA) as SFSObject
            val innerData = data.getSFSObject("data")
            val loginToken = innerData.getUtfString(SFSField.LoginTokenData)
            val loginType = LoginType.from(tryGetField<Int>(innerData, SFSField.LoginType))
            val dataType = DataType.from(tryGetField<String>(innerData, SFSField.DataType))
            val deviceType = DeviceType.from(tryGetField<String>(innerData, SFSField.DeviceType))
            val platform = Platform.from(tryGetField<Int>(innerData, SFSField.Platform))
            val referralCode = tryGetField<String>(innerData, SFSField.REFERRAL_CODE)
            
            identity += " lt=$loginType dt=$dataType d=$deviceType p=$platform"

            // Đảm bảo nếu RON và BAS thì phải thêm network type vào cuối userName
            val userName = UserNameSuffix.tryAddNameSuffix(userNameFromClient, dataType)
            

            // Check coi có force login hay ko
            // Cần check force login trước khi tạo controller để tránh tự kiểm tra với chính account của mình đang login
            val forceLoginService = globalServices.get<ISvServicesContainer>()
                .get(ServerType.from(dataType)).get<IForceLoginManager>()
            forceLoginService.checkToForceLogin(data, userName, dataType,session.hashId)

            val strategy = findStrategy(loginType, dataType)
            val extra = LoginExtraData(loginType, dataType, platform, deviceType, referralCode)
            val userInfo = runBlocking { strategy.login(userName, loginToken, extra) }

            identity += " id=${userInfo.id}"

            // check lại lần nữa vì bây giờ mới có uid để đảm bảo ko login cùng account với network đó
            forceLoginService.checkToKickAccountFi(userInfo.id, userInfo.dataType)


            checkBanOrReview(userInfo, identity)

            runBlocking { strategy.postLogin(userInfo, extra) }
            
            // Prepare data for user
            session.setProperty(SFSField.UserInfo, userInfo)
            session.setProperty(SFSField.NewUser, userInfo.hash.isEmpty()) // do user mới tạo chưa được set hash

            findLogger(loginType, dataType).log("User login success: $identity")
            
            // Track user online status in Redis using UserOnlineManager
            _userOnlineManager.trackUserOnline(userInfo.id, userName, dataType, deviceType, platform)
        } catch (ex: ApiException) {
            val msg = "Error: $identity ${ex.message} ${ex.stackTraceToString()}"
            throw SFSLoginException(msg, SFSErrorData(ServerError.LOGIN_FAILED))
        } catch (ex: SFSLoginException) {
            //Login cùng account ở thiết bị khác
            _logger.error("Login error: $identity}", ex)
            throw ex
        } catch (ex: Exception) {
            val msg = "Error: $identity ${ex.message} ${ex.stackTraceToString()}"
            throw SFSLoginException(msg, SFSErrorData(ServerError.LOGIN_FAILED))
        }
    }

    private fun checkServerMaintenance(userName: String) {
        val isServerMaintenance = _config.serverMaintenance == 1
        if (isServerMaintenance && !isUserInWhitelist(userName)) {
            throw SFSLoginException(
                "Server is in maintenance $userName",
                SFSErrorData(ServerError.SERVER_MAINTENANCE)
            )
        }
    }

    private fun isUserInWhitelist(userName: String): Boolean {
        // FIXME: làm sau
//        val zoneExt = parentExtension as MainGameExtension
//        return zoneExt.userWhiteList.any { username.equals(it, true) }
        return false
    }

    private fun checkBanOrReview(userInfo: IUserInfo, identity: String) {
        if (userInfo.isBanned) {
            val err = SFSErrorData(ServerError.USER_BANNED)
            err.addParameter(userInfo.banExpiredAt.toString()) // ban đến ngày này
            throw SFSLoginException("User is banned: $identity", err)
        }
        if (userInfo.isUnderReviewed) {
            throw SFSLoginException("User in review: $identity", SFSErrorData(ServerError.USER_REVIEW))
        }
    }

    private fun findStrategy(loginType: LoginType?, dataType: DataType?): ILoginStrategy {
        if (dataType != null) {
            return when (dataType) {
                DataType.BSC -> LoginStrategyBnb(globalServices)
                DataType.POLYGON -> LoginStrategyPolygon(globalServices)
                DataType.TR -> LoginStrategyTr(globalServices)
                DataType.GUEST -> LoginStrategyGuest(globalServices)
                DataType.TON -> LoginStrategyTon(globalServices)
                DataType.SOL -> LoginStrategySol(globalServices)
                DataType.RON -> LoginStrategyRon(globalServices)
                DataType.VIC -> LoginStrategyVic(globalServices)
                DataType.BAS -> LoginStrategyBas(globalServices)
                else -> throw Exception("Cannot found strategy for dt $dataType")
            }
        }
        if (loginType != null) {
            return when (loginType) {
                LoginType.BNB_POL -> LoginStrategyTr(globalServices) // Vì ko rõ là BNB hay POL
                LoginType.USERNAME_PASSWORD -> LoginStrategyTr(globalServices)
                LoginType.GUEST -> LoginStrategyGuest(globalServices)
                LoginType.TON -> LoginStrategyTon(globalServices)
                LoginType.SOL -> LoginStrategySol(globalServices)
                else -> throw Exception("Cannot found strategy for lt $loginType")
            }
        }
        throw Exception("Missing dt & lt")
    }

    private fun findLogger(loginType: LoginType?, dataType: DataType?): IServerLogger {
        var sv: ServerType? = null
        if (dataType != null) {
            sv = when (dataType) {
                DataType.TON -> ServerType.TON
                DataType.SOL -> ServerType.SOL
                DataType.RON -> ServerType.RON
                DataType.BAS -> ServerType.BAS
                DataType.VIC -> ServerType.VIC
                else -> ServerType.BNB_POL
            }
        }
        else if (loginType != null) {
            sv = when (loginType) {
                LoginType.TON -> ServerType.TON
                LoginType.SOL -> ServerType.SOL
                else -> ServerType.BNB_POL
            }
        }
        if (sv == null) {
            throw Exception("Missing dt & lt")
        }
        return svServices.get(sv).get<IServerLogger>()
    }
}

data class LoginExtraData(
    val loginType: LoginType,
    val dataType: DataType?,
    val platform: Platform,
    val deviceType: DeviceType,
    val referralCode: String?,
)

interface ILoginStrategy {
    suspend fun login(userName: String, loginToken: String, extra: LoginExtraData): IUserInfo
    suspend fun postLogin(userInfo: IUserInfo, extra: LoginExtraData)
}