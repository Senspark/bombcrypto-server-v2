package com.senspark.game.pvp.info

interface IFallingBlockInfo {
    /** Time since start in milliseconds. */
    val timestamp: Int

    /** Horizontal position. */
    val x: Int

    /** Vertical position. */
    val y: Int
}