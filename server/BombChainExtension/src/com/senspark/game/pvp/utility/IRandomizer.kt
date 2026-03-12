package com.senspark.game.pvp.utility

interface IRandomizer<T> {
    fun random(random: IRandom): T
}