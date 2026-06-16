package com.senspark.game.api

import com.senspark.common.service.IGlobalService
import com.senspark.game.data.model.auth.LegacyLoginInfo
import com.senspark.game.data.model.auth.EtherAirdropLoginInfo
import com.senspark.game.data.model.auth.SolLoginInfo
import com.senspark.game.data.model.auth.TonLoginInfo
import com.senspark.game.declare.EnumConstants

interface IAuthApi : IGlobalService {
    suspend fun verifyAuth(username: String, token: String, dataType: EnumConstants.DataType): LegacyLoginInfo
    suspend fun verifyAuthTr(username: String, token: String): LegacyLoginInfo
    suspend fun verifyMobileUser(username: String, token: String): LegacyLoginInfo
    suspend fun verifyTonUser(username: String, token: String): TonLoginInfo
    suspend fun verifySolUser(walletAddress: String, loginData: String): SolLoginInfo
    suspend fun verifyRonUser(walletAddress: String, loginData: String): EtherAirdropLoginInfo
    suspend fun verifyBasUser(walletAddress: String, loginData: String): EtherAirdropLoginInfo
    suspend fun verifyVicUser(walletAddress: String, loginData: String): EtherAirdropLoginInfo
    suspend fun createAccountSenspark(username: String, password: String, email: String): Int
    suspend fun deleteUser(uid: Int, accessToken: String): Boolean
}