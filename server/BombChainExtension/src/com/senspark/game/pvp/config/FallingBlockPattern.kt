package com.senspark.game.pvp.config

enum class FallingBlockPattern {
    /**
     * Blocks: 104/72/45
     * 1.2.3.4.5.... clockwise start from top-left.
     * 1  2  3  4  5  6
     * 16 17 18 19 20 7
     * 15 24 23 22 21 8
     * 14 13 12 11 10 9
     */
    TopLeftCw,
    TopLeftCcw,

    /**
     * 1.2.3.4.5.... clockwise start from bottom-right.
     * 9 10 11 12 13 14
     * 8 21 22 23 24 15
     * 7 20 19 18 17 16
     * 6 5  4  3  2  1
     */
    BottomRightCw,
    BottomRightCcw,

    /**
     * Blocks: 52/36/23
     * 1  2  3  4 5
     * 8  9 10 11 6
     * 7 12 13 12 7
     * 6 11 10  9 8
     * 5  4  3  2 1
     */
    TopLeftDualCw,
    TopLeftDualCcw,
}