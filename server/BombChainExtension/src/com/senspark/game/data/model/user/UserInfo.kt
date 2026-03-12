package com.senspark.game.data.model.user

import com.senspark.game.declare.EnumConstants.*
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.senspark.game.extension.modules.ServerType
import com.smartfoxserver.v2.entities.data.ISFSObject
import java.security.PrivateKey
import java.time.Instant
import javax.crypto.SecretKey

class UserInfo(obj: ISFSObject) : IUserInfo {
    private val _id: Int
    private var _username: String
    private val _secondUsername: String?
    private val _password: String?
    private var _name: String?
    private var _hash: String
    private val _isBanned: Boolean
    private val _banExpiredAt: Long
    private val _isUnderReviewed: Boolean
    private val _activated: Boolean
    private var _type: UserType
    private var _mode: UserMode
    private var _miningMode: TokenType
    private val _newUser: Boolean
    private val _lastLogout: Instant?
    private lateinit var _dataType: DataType
    private lateinit var _deviceType: DeviceType

    override val id get() = _id
    override var username: String
        get() = _username
        set(value) {
            _username = value
        }
    override val secondUsername get() = _secondUsername
    override val name get() = _name
    override var platform: Platform? = null
    override val hash get() = _hash
    override val isBanned get() = _isBanned
    override val banExpiredAt get() = _banExpiredAt
    override val isUnderReviewed get() = _isUnderReviewed
    override val activated: Boolean get() = _activated
    override var type: UserType
        get() = _type
        set(value) {
            _type = value
        }
    override val newUser get() = _newUser
    override val privilege: UserPrivilege

    override val lastLogout: Instant? get() = _lastLogout
    override var mode: UserMode
        get() = _mode
        set(value) {
            _mode = value
        }

    override lateinit var privateKeyRSA: PrivateKey
    override lateinit var privateKeyRSAStr: String
    override lateinit var aesKey: SecretKey
    override lateinit var serverType: ServerType

    init {
        _id = obj.getInt("id_user")
        _username = obj.getUtfString("user_name")
        _secondUsername = obj.getUtfString("second_username")
        _password = obj.getUtfString("password")
        _name = obj.getUtfString("name")
        _hash = obj.getUtfString("hash")
        if (obj.containsKey("ban_expired_at")) {
            val banExpired = obj.getLong("ban_expired_at")
            _banExpiredAt = banExpired
            _isBanned = obj.getInt("is_ban") != 0 && banExpired > Instant.now().toEpochMilli()
        } else {
            _banExpiredAt = 0
            _isBanned = obj.getInt("is_ban") != 0
        }
        _activated = obj.getInt("activated") == 1
        _isUnderReviewed = obj.getInt("is_review") != 0
        _miningMode = TokenType.valueOf(obj.getUtfString("mining_token"))
        _type = UserType.valueOf(obj.getUtfString("type"))
        _mode = UserMode.valueOf(obj.getUtfString("mode"))
        privilege = UserPrivilege.fromInt(if (obj.containsKey("privilege")) obj.getInt("privilege") else 0)
        if (_type == UserType.TR || _type == UserType.GUEST) {
            _dataType = DataType.TR
        }

        _newUser = if (obj.containsKey("new_user")) obj.getInt("new_user") == 1 else false
        val lastLogout = if (obj.containsKey("original_last_logout")) obj.getLong("original_last_logout") else null
        _lastLogout = lastLogout?.let { Instant.ofEpochSecond(it) }
    }

    override fun setHash(hash: String) {
        _hash = hash
    }

    override fun setName(name: String) {
        _name = name
    }

    override fun isAirdropUser(): Boolean {
        return _dataType == DataType.RON ||
                _dataType == DataType.VIC ||
                _dataType == DataType.SOL ||
                _dataType == DataType.TON ||
                _dataType == DataType.BAS

    }

    override var dataType: DataType
        get() = _dataType
        set(value) {
            if ((setOf(UserType.FI, UserType.SOL).contains(_type) &&
                        setOf(
                            DataType.BSC,
                            DataType.POLYGON,
                            DataType.TON,
                            DataType.SOL,
                            DataType.RON,
                            DataType.VIC,
                            DataType.BAS
                        ).contains(value))
                || (setOf(UserType.TR, UserType.GUEST).contains(_type) && value == DataType.TR)
            ) {
                _dataType = value
            } else {
                throw CustomException("Cannot set data type $_dataType for user $_type", ErrorCode.INVALID_PARAMETER)
            }
        }

    override var deviceType: DeviceType
        get() = _deviceType
        set(value) {
            _deviceType = value
        }
}
