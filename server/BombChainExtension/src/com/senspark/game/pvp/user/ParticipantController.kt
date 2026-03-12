package com.senspark.game.pvp.user

import com.senspark.common.pvp.IMatchUserInfo
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.Booster
import com.senspark.game.pvp.manager.ITimeManager
import com.smartfoxserver.v2.entities.User

class ParticipantController(
    override val info: IMatchUserInfo,
    override val teamId: Int,
    private val _slot: Int,
    private val _logger: ILogger,
    private val _timeManager: ITimeManager,
) : IParticipantController {
    private var _user: User? = null
    private val _userLocker = Any()
    private var _ready = false
    private var _quited = false

    private val _boosters = info.availableBoosters.map {
        val booster = Booster.fromValue(it.key)
        UserBooster(
            item = booster,
            cooldown = booster.coolDown,
            _quantity = it.value,
            _timeManager = _timeManager
        )
    }

    override val user: User?
        get() {
            synchronized(_userLocker) {
                return _user
            }
        }
    override val isReady get() = _ready
    override val isQuited get() = _quited

    override val usedBoosters
        get() = _boosters
            .filter { it.useTimes > 0 }
            .associate { it.item.value to it.useTimes }

    override fun join(user: User) {
        _logger.log("[ParticipantController:join] slot=$_slot")
        synchronized(_userLocker) {
            _user = user
        }
    }

    override fun leave() {
        _logger.log("[ParticipantController:leave] slot=$_slot")
        synchronized(_userLocker) {
            _user = null
        }
    }

    override fun ready() {
        _ready = true
    }

    override fun quit() {
        _quited = true
    }

    override fun useBooster(booster: Booster) {
        val item = _boosters.firstOrNull { it.item == booster }
            ?: throw Exception("Not found booster id: $booster")
        item.use()
    }

    override fun reset() {
        _ready = false
        _quited = false
        // FIXME: reset booster cooldown.
    }
}