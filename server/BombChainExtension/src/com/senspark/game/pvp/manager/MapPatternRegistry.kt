package com.senspark.game.pvp.manager

object MapPatternRegistry {
    private val patterns = mutableMapOf<String, String>()

    init {
        // SMALL (15x11) - Optimized for 1v1
        patterns["SMALL_1V1"] = """
            0_b.........b_1
            _x.x.x.x.x.x.x_
            b.............b
            .x.x.x.x.x.x.x.
            ...............
            .x.x.x.x.x.x.x.
            ...............
            .x.x.x.x.x.x.x.
            b.............b
            _x.x.x.x.x.x.x_
            1_b.........b_0
        """.trimIndent()

        // MEDIUM (21x15) - For 2v2 and 3v3
        patterns["MEDIUM_TEAM"] = """
            0_b...............b_1
            _x.x.x.x.x.x.x.x.x.x_
            b...................b
            .x.x.x.x.x.x.x.x.x.x.
            .....................
            .x.x.x.x.x.x.x.x.x.x.
            2_b...............b_2
            .x.x.x.x.x.x.x.x.x.x.
            .....................
            .x.x.x.x.x.x.x.x.x.x.
            b...................b
            _x.x.x.x.x.x.x.x.x.x_
            1_b...............b_0
        """.trimIndent()

        // LARGE (31x21) - For Battle Royale 6P
        patterns["LARGE_BR"] = """
            0_b...........................b_1
            _x.x.x.x.x.x.x.x.x.x.x.x.x.x.x_
            b.............................b
            .x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.
            ...............................
            .x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.
            4_b...........................b_5
            .x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.
            ...............................
            .x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.
            ...............................
            .x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.
            ...............................
            .x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.
            2_b...........................b_3
            .x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.
            ...............................
            .x.x.x.x.x.x.x.x.x.x.x.x.x.x.x.
            b.............................b
            _x.x.x.x.x.x.x.x.x.x.x.x.x.x.x_
            1_b...........................b_0
        """.trimIndent()
    }

    fun getPattern(id: String): String {
        return patterns[id] ?: patterns["SMALL_1V1"]!!
    }
}
