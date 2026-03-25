package com.senspark.game.api.login

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.DeviceType

interface ILoginManager : IServerService {
    /**
     * Logs in as a linked user.
     * @param authorizationToken JWT token (from client).
     * @param dataType TR/BSC/POLYGON.
     * @param deviceType MOBILE/WEB.
     */
    suspend fun loginAccount(
        username: String,
        authorizationToken: String,
        dataType: EnumConstants.DataType?,
        deviceType: DeviceType,
    ): IUserInfo

    /**
     * Logs in as a guest user.
     * @param username Guest username.
     */
    suspend fun loginGuest(username: String, token: String): IUserInfo

    /**
     * Login dành riêng cho user TON (Telegram)
     */
    suspend fun loginTon(userName: String, loginTokenData: String, deviceType: DeviceType): IUserInfo

    /**
     * Login dành riêng cho Solana
     */
    suspend fun loginSol(walletAddress: String, loginData: String, deviceType: DeviceType): IUserInfo

    /**
     * Login dành riêng cho Ron (Ronin)
     */
    suspend fun loginRon(walletAddress: String, loginData: String, deviceType: DeviceType): IUserInfo
    
    /**
     * Login dành riêng cho Bas (Base)
     */
    suspend fun loginBas(walletAddress: String, loginData: String, deviceType: DeviceType): IUserInfo

    /**
     * Login dành riêng cho Vic (Viction)
     */
    suspend fun loginVic(walletAddress: String, loginData: String, deviceType: DeviceType): IUserInfo
}