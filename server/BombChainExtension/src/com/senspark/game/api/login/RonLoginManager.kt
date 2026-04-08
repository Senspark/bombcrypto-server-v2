package com.senspark.game.api.login

import com.senspark.game.api.IAuthApi
import com.senspark.game.data.model.auth.UserLoginInfo
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.UserNameSuffix
import com.senspark.game.utils.AesEncryption

class RonLoginManager(
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
        throw Exception("Not supported")
    }

    override suspend fun loginSol(walletAddress: String, loginData: String, deviceType: EnumConstants.DeviceType): IUserInfo {
        throw Exception("Not supported")
    }

    override suspend fun loginRon(
        walletAddress: String,
        loginData: String,
        deviceType: EnumConstants.DeviceType
    ): IUserInfo {
        val info = _authApi.verifyRonUser(walletAddress, loginData)
        val user = _userDataAccess.saveUserLoginInfo(
            UserLoginInfo(
                info.userId,
                info.walletAddress,
                null,
                null,
                null,
                EnumConstants.UserType.FI,
                false,
                info.createAt,
            ), deviceType
        )
        user.dataType = EnumConstants.DataType.RON
        user.deviceType = deviceType
        user.aesKey = AesEncryption.importKeyFromBase64(info.aesKey)
        user.username = UserNameSuffix.tryAddNameSuffix(user.username, EnumConstants.DataType.RON)

        return user
    }
    
    override suspend fun loginBas(
        walletAddress: String,
        loginData: String,
        deviceType: EnumConstants.DeviceType
    ): IUserInfo {
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
