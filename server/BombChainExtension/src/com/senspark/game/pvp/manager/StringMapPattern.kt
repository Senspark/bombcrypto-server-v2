package com.senspark.game.pvp.manager

class StringMapPattern(
    private val _pattern: String,
) : IMapPattern {
    private val _items: List<MutableList<Char>> = _pattern
        .split('\n')
        .map { it.toMutableList() }
        .reversed()

    override val width = _items.maxBy { it.size }.size
    override val height = _items.size

    override fun get(x: Int, y: Int): Char {
        val row = _items[y]
        return row.getOrElse(x) { ' ' } // Empty char.
    }

    override fun set(x: Int, y: Int, c: Char) {
        _items[y][x] = c
    }

    override fun find(predicate: (x: Int, y: Int, c: Char) -> Boolean): List<Pair<Int, Int>> {
        val positions = (0 until height).map { y ->
            (0 until width).map col@{ x ->
                val item = get(x, y)
                if (predicate(x, y, item)) {
                    Pair(x, y)
                } else {
                    null
                }
            }
        }
            .flatten()
            .filterNotNull()
        return positions
    }
}