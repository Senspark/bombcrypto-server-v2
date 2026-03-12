package com.senspark.game.data.model.user

import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.extension.modules.ServerType
import java.security.PrivateKey
import java.time.Instant
import javax.crypto.SecretKey

class ReplicaUserInfo(
    val info: IUserInfo,
    newId: Int,
) : IUserInfo {
    override val id = newId

    override var username: String
        get() = info.username
        set(value) {
            info.username = value
        }

    override val secondUsername: String?
        get() = info.secondUsername

    override val name: String?
        get() = info.name

    override var platform: EnumConstants.Platform?
        get() = info.platform
        set(value) {
            info.platform = value
        }

    override val hash: String
        get() = info.hash

    override val isBanned: Boolean
        get() = info.isBanned

    override val banExpiredAt: Long
        get() = info.banExpiredAt

    override val isUnderReviewed: Boolean
        get() = info.isUnderReviewed

    override val activated: Boolean
        get() = info.activated

    override var type: EnumConstants.UserType
        get() = info.type
        set(value) {
            info.type = value
        }

    override var mode: EnumConstants.UserMode
        get() = info.mode
        set(value) {
            info.mode = value
        }
    override val privilege: UserPrivilege
        get() = info.privilege

    override var dataType: EnumConstants.DataType
        get() = info.dataType
        set(value) {
            info.dataType = value
        }

    override var deviceType: EnumConstants.DeviceType
        get() = info.deviceType
        set(value) {
            info.deviceType = value
        }

    override val newUser: Boolean
        get() = info.newUser

    override val lastLogout: Instant?
        get() = info.lastLogout

    override var privateKeyRSA: PrivateKey
        get() = info.privateKeyRSA
        set(value) {
            info.privateKeyRSA = value
        }

    override var privateKeyRSAStr: String
        get() = info.privateKeyRSAStr
        set(value) {
            info.privateKeyRSAStr = value
        }

    override var aesKey: SecretKey
        get() = info.aesKey
        set(value) {
            info.aesKey = value
        }

    override var serverType: ServerType
        get() = info.serverType
        set(value) {
            info.serverType = value
        }

    override fun isAirdropUser(): Boolean {
        return info.dataType == DataType.RON ||
                info.dataType == DataType.VIC ||
                info.dataType == DataType.SOL ||
                info.dataType == DataType.TON ||
                info.dataType == DataType.BAS

    }

    override fun setHash(hash: String) {
        info.setHash(hash)
    }

    override fun setName(name: String) {
        info.setName(name)
    }

}