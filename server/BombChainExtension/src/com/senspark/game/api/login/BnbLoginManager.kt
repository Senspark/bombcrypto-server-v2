package com.senspark.game.api.login

import com.senspark.game.api.IAuthApi
import com.senspark.game.data.model.auth.IUserLoginInfo
import com.senspark.game.data.model.auth.LegacyLoginInfo
import com.senspark.game.data.model.auth.UserLoginInfo
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.UserNameSuffix
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.user.IUserLinkManager
import com.senspark.game.utils.AesEncryption

class BnbLoginManager(
    private val _authApi: IAuthApi,
    private val _userDataAccess: IUserDataAccess,
    private val _userLinkManager: IUserLinkManager
) : ILoginManager {
    override fun initialize() {
    }
    
    override fun loginAccount(
        username: String,
        authorizationToken: String,
        dataType: EnumConstants.DataType?,
        deviceType: EnumConstants.DeviceType
    ): IUserInfo {
        if(dataType == null){
            throw CustomException("Not have dataType", ErrorCode.INVALID_PARAMETER)
        }
        val info: LegacyLoginInfo;
        if (deviceType == EnumConstants.DeviceType.MOBILE) {
            info = _authApi.verifyMobileUser(username, authorizationToken)
        }
        else if (dataType == EnumConstants.DataType.TR){
            info = _authApi.verifyAuthTr(username, authorizationToken)
        }
        else {
            info = _authApi.verifyAuth(username, authorizationToken, dataType)
        }
        val userLoginInfo = UserLoginInfo(
            info.userId,
            if (info.address.isNullOrEmpty()) info.userName!! else info.address.lowercase(),
            info.userName,
            if (info.nickName.isNullOrEmpty()) null else info.nickName,
            null,
            if (info.isUserFi) EnumConstants.UserType.FI else EnumConstants.UserType.TR,
            false,
            info.createAt,
        )
        if (userLoginInfo.userType == EnumConstants.UserType.FI) {
            if (!(dataType == EnumConstants.DataType.BSC || dataType == EnumConstants.DataType.POLYGON)) {
                throw CustomException("Data type invalid: $dataType", ErrorCode.INVALID_PARAMETER)
            }
        }

        val linkedInfo = getLinkedToUserInfo(userLoginInfo)
        val userInfo = if (linkedInfo.userId == userLoginInfo.userId) {
            _userDataAccess.saveUserLoginInfo(userLoginInfo, deviceType)
        } else {
            _userDataAccess.getUserInfo(linkedInfo, deviceType)
        }
        // Đây là account guest link qua TR rồi link Wallet
        if(userInfo.type != userLoginInfo.userType && userLoginInfo.userType == EnumConstants.UserType.FI) {
            // Do cách link account guest trong db vẫn giữ userType là guest nên cần update lại ở đây để
            // đảm bảo user này vẫn đúng userType dù có lấy profile của acc Guest link qua
            userInfo.type = userLoginInfo.userType
            val name = info.address ?: userInfo.username
            userInfo.username = UserNameSuffix.tryAddNameSuffix(name, dataType)
        }
        
        if (userInfo.type == EnumConstants.UserType.FI) {
            userInfo.dataType = dataType
        }
        userInfo.deviceType = deviceType
        userInfo.aesKey = AesEncryption.importKeyFromBase64(info.aesKey)
        userInfo.username = UserNameSuffix.tryAddNameSuffix(userInfo.username, dataType)
        return userInfo
    }

    override fun loginGuest(username: String, token: String): IUserInfo {
        val info = _authApi.verifyMobileUser(username, token)
        val userLoginInfo = UserLoginInfo(
            info.userId,
            info.userName!!,
            info.userName,
            null,
            null,
            EnumConstants.UserType.GUEST,
            false,
            info.createAt,
        )
        val deviceType = EnumConstants.DeviceType.MOBILE
        val user = _userDataAccess.saveUserLoginInfo(userLoginInfo, deviceType)
        user.dataType = EnumConstants.DataType.TR
        user.deviceType = deviceType
        user.aesKey = AesEncryption.importKeyFromBase64(info.aesKey)
        return user
    }

    override fun loginTon(userName: String, loginTokenData: String, deviceType: EnumConstants.DeviceType): IUserInfo {
        throw Exception("Not supported")
    }

    override fun loginSol(walletAddress: String, loginData: String, deviceType: EnumConstants.DeviceType): IUserInfo {
        throw Exception("Not supported")
    }

    override fun loginRon(
        walletAddress: String,
        loginData: String,
        deviceType: EnumConstants.DeviceType
    ): IUserInfo {
        throw Exception("Not supported");
    }
    
    override fun loginBas(
        walletAddress: String,
        loginData: String,
        deviceType: EnumConstants.DeviceType
    ): IUserInfo {
        throw Exception("Not supported")
    }

    override fun loginVic(
        walletAddress: String,
        loginData: String,
        deviceType: EnumConstants.DeviceType
    ): IUserInfo {
        TODO("Not yet implemented")
    }

    private fun getLinkedToUserInfo(info: IUserLoginInfo): IUserLoginInfo {
        val linkedToUserId = _userLinkManager.getLinkedToUserId(info.userId)
        if (linkedToUserId == info.userId) {
            // No change.
            return info
        }
        return UserLoginInfo(
            userId = linkedToUserId,
            username = info.username, // No effect.
            loginUsername = info.loginUsername, // No effect.
            displayName = info.displayName,
            email = info.email,
            userType = info.userType,
            hasPasscode = info.hasPasscode,
            createAt = info.createAt,
        )
    }
}