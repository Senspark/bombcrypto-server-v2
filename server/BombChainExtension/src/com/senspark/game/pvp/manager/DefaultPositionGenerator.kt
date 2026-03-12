package com.senspark.game.pvp.manager

class DefaultPositionGenerator(
    private val _chars: List<Char>,
) : IPositionGenerator {
    override fun generate(pattern: IMapPattern): List<Pair<Int, Int>> {
        return _chars.map {
            pattern.find { _, _, c -> c == it }
        }.flatten()
    }
}