package com.senspark.game.data.model.auth

import com.senspark.game.declare.EnumConstants.UserType
import kotlinx.serialization.Serializable

interface IUserLoginInfo {
    /** Unique numeric user ID (auto-generated). */
    val userId: Int

    /** Unique text user ID (wallet address/facebook ID...). */
    val username: String

    /** Unique user-generated text ID, used to login. */
    val loginUsername: String?

    /** Display name. */
    val displayName: String?

    /** Email, used by senspark account. */
    val email: String?

    val userType: UserType

    val hasPasscode: Boolean
    val createAt: Long
}

@Serializable
data class SolLoginInfo(
    val userId: Int,
    val walletAddress: String,
    val createAt: Long,
    val aesKey: String,
    val extraData: String
)

@Serializable
data class TonLoginInfo(
    val userId: Int,
    val walletAddress: String,
    val telegramUserName: String? = "",
    val aesKey: String,
    val createAt: Long
)

@Serializable
data class LegacyLoginInfo(
    val userId: Int,
    val userName: String?,
    val address: String?,
    val isUserFi: Boolean,
    val nickName: String?,
    val createAt: Long,
    val aesKey: String,
    val extraData: String
)

@Serializable
data class EtherAirdropLoginInfo(
    val userId: Int,
    val walletAddress: String,
    val createAt: Long,
    val aesKey: String,
    val extraData: String
)

