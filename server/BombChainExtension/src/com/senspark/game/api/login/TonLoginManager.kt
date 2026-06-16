package com.senspark.game.api.login

import com.senspark.game.api.IAuthApi
import com.senspark.game.data.model.auth.UserLoginInfo
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.utils.AesEncryption

class TonLoginManager(
    private val _authApi: IAuthApi,
    private val _userDataAccess: IUserDataAccess,
) : ILoginManager {
    override fun initialize() {
    }
    
    override suspend fun loginAccount(
        username: String,
        authorizationToken: String,
        dataType: EnumConstants.DataType?,
        deviceType: EnumConstants.DeviceType
    ): IUserInfo {
        throw Exception("Not supported")
    }

    override suspend fun loginGuest(username: String, token: String): IUserInfo {
        throw Exception("Not supported")
    }

    override suspend fun loginTon(userName: String, loginTokenData: String, deviceType: EnumConstants.DeviceType): IUserInfo {
        val info = _authApi.verifyTonUser(userName, loginTokenData)
        val userInfo = UserLoginInfo(
            userId = info.userId,
            username = info.walletAddress,
            loginUsername = null,
            displayName = info.telegramUserName?.ifBlank { null },
            email = null,
            userType = EnumConstants.UserType.FI,
            hasPasscode = false,
            createAt = info.createAt,
        )
        val user = _userDataAccess.saveUserLoginInfo(userInfo, deviceType)
        _userDataAccess.updateLogoutInfo(info.userId, deviceType)
        user.dataType = EnumConstants.DataType.TON
        user.deviceType = deviceType
        user.aesKey = AesEncryption.importKeyFromBase64(info.aesKey)
        return user
    }

    override suspend fun loginSol(walletAddress: String, loginData: String, deviceType: EnumConstants.DeviceType): IUserInfo {
        throw Exception("Not supported")
    }

    override suspend fun loginRon(
        walletAddress: String,
        loginData: String,
        deviceType: EnumConstants.DeviceType
    ): IUserInfo {
        throw Exception("Not supported")
    }
    
    override suspend fun loginBas(walletAddress: String, loginData: String, deviceType: EnumConstants.DeviceType): IUserInfo {
        throw Exception("Not supported")
    }

    override suspend fun loginVic(
        walletAddress: String,
        loginData: String,
        deviceType: EnumConstants.DeviceType
    ): IUserInfo {
        throw Exception("Not supported")
    }
}