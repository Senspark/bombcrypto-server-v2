package com.senspark.game.api

import com.senspark.common.service.IGlobalService
import com.senspark.game.data.model.auth.LegacyLoginInfo
import com.senspark.game.data.model.auth.EtherAirdropLoginInfo
import com.senspark.game.data.model.auth.SolLoginInfo
import com.senspark.game.data.model.auth.TonLoginInfo
import com.senspark.game.declare.EnumConstants

interface IAuthApi : IGlobalService {
    fun verifyAuth(username: String, token: String, dataType: EnumConstants.DataType): LegacyLoginInfo
    fun verifyAuthTr(username: String, token: String): LegacyLoginInfo
    fun verifyMobileUser(username: String, token: String): LegacyLoginInfo
    fun verifyTonUser(username: String, token: String): TonLoginInfo
    fun verifySolUser(walletAddress: String, loginData: String): SolLoginInfo
    fun verifyRonUser(walletAddress: String, loginData: String): EtherAirdropLoginInfo
    fun verifyBasUser(walletAddress: String, loginData: String): EtherAirdropLoginInfo
    fun verifyVicUser(walletAddress: String, loginData: String): EtherAirdropLoginInfo
    fun createAccountSenspark(username: String, password: String, email: String): Int
    fun deleteUser(uid: Int, accessToken: String): Boolean
}