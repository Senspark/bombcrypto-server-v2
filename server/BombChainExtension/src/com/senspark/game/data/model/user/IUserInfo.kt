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
    /** Real wallet address (username may have suffix by network, walletAddress do not) */
    val walletAddress: String?

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

    /**
     * True nếu account GỐC là FI, kể cả khi phiên đã bị [forceAdventureTr] ép `type=TR`.
     * `type` bị overload 2 vai trò: routing/economy (ép TR) và permission gate (FI-only). Cờ này
     * cho phép permission gate (vd chợ V3 sell/edit/cancel) phân biệt FI-bị-ép-adventure với TR/guest THẬT.
     */
    val isOriginallyFi: Boolean

    /**
     * Adventure/PvP là game TR (hero TR + kinh tế TR riêng biệt). User FI khi vào mode ADVENTURE:
     * sau khi auth bằng network thật (BSC/POLYGON) thành công thì ép phiên sang TR (type + dataType,
     * bỏ walletAddress) để rơi vào xô (uid, TR) — tách hẳn treasure (uid, network) ở mọi registry key
     * theo (uid, dataType) (vd AllHeroesFiManager) mà không cần thread landing. Gọi SAU khi login xong.
     */
    fun forceAdventureTr()
    fun setHash(hash: String)
    fun setName(name: String)
}