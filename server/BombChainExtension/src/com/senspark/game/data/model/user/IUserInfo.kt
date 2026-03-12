package com.senspark.game.data.model.user

import com.senspark.game.declare.EnumConstants.*
import com.senspark.game.extension.modules.ServerType
import java.security.PrivateKey
import java.time.Instant
import javax.crypto.SecretKey

interface IUserInfo {
    /** Unique ID in SQL database. */
    val id: Int

    /** Unique wallet address */
    var username: String
    val secondUsername: String?

    /** Display name. */
    val name: String?
    val displayName: String get() = name ?: secondUsername ?: username

    var platform: Platform?

    /** Session hash. */
    val hash: String
    val isBanned: Boolean
    val banExpiredAt: Long
    val isUnderReviewed: Boolean
    val activated: Boolean
    var type: UserType
    var mode: UserMode
    val privilege: UserPrivilege
    var dataType: DataType
    var deviceType: DeviceType
    val newUser: Boolean
    val lastLogout: Instant?
    var privateKeyRSA: PrivateKey
    var privateKeyRSAStr: String
    var aesKey: SecretKey
    var serverType: ServerType
    
    fun isAirdropUser(): Boolean
    fun setHash(hash: String)
    fun setName(name: String)
}