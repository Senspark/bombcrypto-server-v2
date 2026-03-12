package com.senspark.game.pvp.user

import com.senspark.common.utils.ILogger
import com.smartfoxserver.v2.entities.User

class ObserverController(
    private val _slot: Int,
    private val _logger: ILogger,
) : IObserverController {
    private var _user: User? = null
    private val _userLocker = Any()

    override val user: User?
        get() {
            synchronized(_userLocker) {
                return _user
            }
        }

    override fun join(user: User) {
        _logger.log("[ObserverController:join] slot=$_slot")
        synchronized(_userLocker) {
            _user = user
        }
    }

    override fun leave() {
        _logger.log("[ObserverController:leave] slot=$_slot")
        synchronized(_userLocker) {
            _user = null
        }
    }
}