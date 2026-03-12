package com.senspark.game.data.model.user

import com.senspark.game.data.model.config.Position
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class AdventureEnemy(
    val enemyId: Int,
    val skin: Int,
    val damage: Int,
    val range: Int,
    private val spawnPosition: Position,
    private val speed: Float,
    private val follow: Boolean,
    private var _health: Float,
    private val throughBrick: Boolean
) {
    val health get() = _health
    private val maxHealth = _health

    fun isAlive(): Boolean {
        return _health > 0f
    }

    fun die() {
        _health = 0f
    }

    fun subHealth(value: Int) {
        _health -= value
    }

    fun toObject(): ISFSObject {
        return SFSObject().apply {
            putInt("id", enemyId)
            putInt("skin", skin)
            putInt("damage", damage)
            putInt("bomb_range", range)
            putFloat("speed", speed)
            putBool("follow", follow)
            putFloat("hp", _health)
            putFloat("maxHp", _health)
            putBool("throughBrick", throughBrick)
            putInt("bombSkin", 0)
            putSFSObject("spawn", spawnPosition.toSfsObject())
        }
    }
}