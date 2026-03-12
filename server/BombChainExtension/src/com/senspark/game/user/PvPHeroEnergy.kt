package com.senspark.game.user

import com.senspark.common.utils.ILogger
import com.senspark.game.constant.PvPConstants
import com.senspark.game.data.PvPHeroEnergyData
import com.smartfoxserver.v2.entities.data.SFSObject
import java.time.Instant
import java.time.temporal.ChronoUnit

class PvPHeroEnergy(
    private val _data: PvPHeroEnergyData,
    private val _logger: ILogger
) {
    val heroId = _data.heroId

    init {
        update()
    }

    fun addEnergy(quantity: Int) {
        _data.balance += quantity
    }

    fun getBalance(): Int {
        update()
        return _data.balance
    }

    fun subEnergy(quantity: Int) {
        _logger.log("[Pvp][PvpHeroEnergy:subEnergy] quantity: $quantity")
        update()
        _logger.log("[Pvp][PvpHeroEnergy:subEnergy] quantity: $quantity/${_data.balance}")
        if (_data.balance < quantity) {
            val msg = "Not enough energy: $heroId"
            _logger.log("[Pvp] $msg")
            throw Exception(msg)
        }
        _data.balance -= quantity
    }

    fun toJson(): Any {
        return _data
    }

    fun toSFSObject(): SFSObject {
        val result = SFSObject()
        val duration = _data.lastSyncTimestamp.until(Instant.now(), ChronoUnit.MILLIS)
        val regenerationRate = PvPConstants.PVP_REGENERATION_RATE * 60 * 1000
        result.putLong("id", heroId.toLong())
        result.putInt("balance", getBalance())
        result.putLong("last_sync_timestamp", _data.lastSyncTimestamp.toEpochMilli())
        result.putLong(
            "remaining_time",
            if (_data.balance < PvPConstants.PVP_HERO_MAX_ENERGY) regenerationRate - duration else 0
        )
        return result
    }

    private fun update() {
        val time = Instant.now()
        var duration = _data.lastSyncTimestamp.until(time, ChronoUnit.MINUTES)
        for (i in _data.balance until PvPConstants.PVP_HERO_MAX_ENERGY) {
            if (duration < PvPConstants.PVP_REGENERATION_RATE) {
                break
            }
            duration -= PvPConstants.PVP_REGENERATION_RATE
            _data.balance++
        }
        _data.lastSyncTimestamp =
            if (_data.balance < PvPConstants.PVP_HERO_MAX_ENERGY) time.minus(duration, ChronoUnit.MINUTES) else time
    }
}