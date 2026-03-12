package com.senspark.game.pvp.delta

interface IHeroStateDelta {
    val slot: Int
    val base: StateDelta<List<Long>>?
    val position: StateDelta<Long>?
}