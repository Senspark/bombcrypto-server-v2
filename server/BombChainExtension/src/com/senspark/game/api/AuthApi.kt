package com.senspark.game.api

import com.senspark.common.utils.IServerLogger
import com.senspark.game.data.model.auth.*
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.UserNameSuffix
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.extension.modules.ServerType
import com.senspark.game.manager.IEnvManager
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*

class AuthApi(
    private val _envManager: IEnvManager,
    svServices: ISvServicesContainer,
) : IAuthApi {

    private var _jwt: String = ""
    private val _loggerBnb: IServerLogger by lazy { svServices.get(ServerType.BNB_POL).get<IServerLogger>() }
    private val _loggerTon: IServerLogger by lazy { svServices.get(ServerType.TON).get<IServerLogger>() }
    private val _loggerSol: IServerLogger by lazy { svServices.get(ServerType.SOL).get<IServerLogger>() }
    private val _loggerRon: IServerLogger by lazy { svServices.get(ServerType.RON).get<IServerLogger>() }
    private val _loggerBas: IServerLogger by lazy { svServices.get(ServerType.BAS).get<IServerLogger>() }
    private val _loggerVic: IServerLogger by lazy { svServices.get(ServerType.VIC).get<IServerLogger>() }

    override fun initialize() {
        _jwt = _envManager.apLoginToken
    }

    override fun verifyAuth(username: String, token: String, dataType: EnumConstants.DataType): LegacyLoginInfo {
        val url = if(dataType == DataType.POLYGON) _envManager.polVerifyLoginUrl else _envManager.bscVerifyLoginUrl
        val body = buildJsonObject {
            put("walletAddress", UserNameSuffix.removeSuffixName(username))
            put("loginData", token)
        }
        val response = StandardSensparkApi.post(url, _jwt, body, LegacyLoginInfo.serializer(), _loggerBnb)
        return response!!
    }

    override fun verifyAuthTr(
        username: String,
        token: String
    ): LegacyLoginInfo {
        val url = _envManager.webVerifyLoginUrl
        val body = buildJsonObject {
            put("walletAddress", username)
            put("loginData", token)
        }
        val response = StandardSensparkApi.post(url, _jwt, body, LegacyLoginInfo.serializer(), _loggerBnb)
        return response!!
    }

    override fun verifyMobileUser(username: String, token: String): LegacyLoginInfo {
        val url = String.format(_envManager.mobileVerifyUrl, username)
        val body = buildJsonObject {
            put("userName", username)
            put("loginData", token)
        }
        val response = StandardSensparkApi.post(url, _jwt, body, LegacyLoginInfo.serializer(), _loggerBnb)
        return response!!
    }

    override fun verifyTonUser(username: String, token: String): TonLoginInfo {
        val url = _envManager.tonVerifyLoginUrl
        val body = buildJsonObject {
            put("walletAddress", username)
            put("loginData", token)
        }
        val response = StandardSensparkApi.post(url, _jwt, body, TonLoginInfo.serializer(), _loggerTon)
        return response!!
    }

    override fun verifySolUser(walletAddress: String, loginData: String): SolLoginInfo {
        val url = _envManager.solVerifyLoginUrl
        val body = buildJsonObject {
            put("walletAddress", walletAddress)
            put("loginData", loginData)
        }
        val response = StandardSensparkApi.post(url, _jwt, body, SolLoginInfo.serializer(), _loggerSol)
        return response!!
    }

    override fun verifyRonUser(
        walletAddress: String,
        loginData: String
    ): EtherAirdropLoginInfo {
        val url = _envManager.ronVerifyLoginUrl
        val body = buildJsonObject {
            put("walletAddress", UserNameSuffix.removeSuffixName(walletAddress))
            put("loginData", loginData)
        }
        val response = StandardSensparkApi.post(url, _jwt, body, EtherAirdropLoginInfo.serializer(), _loggerRon)
        return response!!
    }

    override fun verifyBasUser(
        walletAddress: String,
        loginData: String
    ): EtherAirdropLoginInfo {
        val url = _envManager.basVerifyLoginUrl
        val body = buildJsonObject {
            put("walletAddress", UserNameSuffix.removeSuffixName(walletAddress))
            put("loginData", loginData)
        }
        val response = StandardSensparkApi.post(url, _jwt, body, EtherAirdropLoginInfo.serializer(), _loggerBas)
        return response!!
    }

    override fun verifyVicUser(
        walletAddress: String,
        loginData: String
    ): EtherAirdropLoginInfo {
        val url = _envManager.vicVerifyLoginUrl
        val body = buildJsonObject {
            put("walletAddress", UserNameSuffix.removeSuffixName(walletAddress))
            put("loginData", loginData)
        }
        val response = StandardSensparkApi.post(url, _jwt, body, EtherAirdropLoginInfo.serializer(), _loggerVic)
        return response!!
    }

    override fun createAccountSenspark(username: String, password: String, email: String): Int {
        val url = _envManager.mobileCreateAccount
        val body = buildJsonObject {
            put("username", username)
            put("password", password)
            put("email", email)
        }
        val response = StandardSensparkApi.post(url, _jwt, body, Int.serializer(), _loggerBnb)
        return response!!
    }

    override fun deleteUser(uid: Int, accessToken: String): Boolean {
        throw Exception("This function is under maintenance")
//        val url = "${_envManager.apiDomainUrl}/gateway/auth/tr/delete-user"
//        val body = Json.run {
//            buildJsonObject {
//                put("userId", uid)
//                put("accessToken", accessToken)
//            }
//        }
//        val bodyJson = _api.delete<>(url, _jwt, body)
//        val json = Json.parseToJsonElement(bodyJson).jsonObject
//        val code = json["statusCode"]?.jsonPrimitive?.int ?: throw CustomException("Missing statusCode")
//        if (code != 0) {
//            throw CustomException("Cannot delete user, code $code")
//        }
//        return json["message"]?.jsonObject?.get("deleted")?.jsonPrimitive?.boolean
//            ?: throw CustomException("Invalid response data ${json["message"]}")

    }
}