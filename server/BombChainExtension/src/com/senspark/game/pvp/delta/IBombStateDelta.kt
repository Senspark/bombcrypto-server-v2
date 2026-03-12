package com.senspark.game.pvp.delta

interface IBombStateDelta {
    val id: Int
    val state: List<Long>
    val lastState: List<Long>
}