package com.senspark.game.pvp.manager

interface IMapPattern {
    val width: Int
    val height: Int
    fun get(x: Int, y: Int): Char
    fun set(x: Int, y: Int, c: Char)
    fun find(predicate: (x: Int, y: Int, c: Char) -> Boolean): List<Pair<Int, Int>>
}