package com.senspark.game.pvp.manager

interface IPositionGenerator {
    fun generate(pattern: IMapPattern): List<Pair<Int, Int>>
}