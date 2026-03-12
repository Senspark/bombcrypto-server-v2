package com.senspark.game.data.model.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class BlockMap {
    var i = 0
    var j = 0
    var type = 0
    var hp = 0
    var maxHp = 0

    constructor() {}
    constructor(i: Int, j: Int, type: Int, hp: Int, maxHp: Int) {
        this.i = i
        this.j = j
        this.type = type
        this.hp = hp
        this.maxHp = maxHp
    }

    fun subHP(hpSub: Int) {
        hp -= hpSub
        if (hp <= 0) hp = 0
    }
}