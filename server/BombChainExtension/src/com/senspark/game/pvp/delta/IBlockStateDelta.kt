package com.senspark.game.pvp.delta

interface IBlockStateDelta {
    val x: Int
    val y: Int
    val state: Long
    val lastState: Long
}