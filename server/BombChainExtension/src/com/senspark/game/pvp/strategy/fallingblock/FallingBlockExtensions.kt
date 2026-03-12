package com.senspark.game.pvp.strategy.fallingblock

import com.senspark.game.pvp.config.FallingBlockPattern
import com.senspark.game.pvp.strategy.matrix.*

fun FallingBlockPattern.toGenerator(): IFallingBlockGenerator {
    val spiralStrategy = { side: MatrixSide,
                           ccw: Boolean,
                           shrinkNextSide: Boolean ->
        val sides = side.nextSides(ccw, 0, 4)
        ListMatrixStrategy(
            *sides.map {
                val items = mutableListOf(ProcessMatrixStrategy(it, ccw), ShrinkMatrixStrategy(it))
                if (shrinkNextSide) {
                    val oppositeSide = it.nextSides(ccw, 2, 1)[0]
                    items.add(ShrinkMatrixStrategy(oppositeSide))
                }
                ListMatrixStrategy(*items.toTypedArray())
            }.toTypedArray()
        )
    }
    val generator = when (this) {
        FallingBlockPattern.TopLeftCw -> MultiFallingBlockGenerator(listOf(
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Top, false, false) loop 2, 0, 0.5f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Top, false, false) loop 2, 2, 0.75f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Top, false, false) loop 3, 4, 1f, 140),
        ))

        FallingBlockPattern.TopLeftCcw -> MultiFallingBlockGenerator(listOf(
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Left, true, false) loop 2, 0, 0.5f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Left, true, false) loop 2, 2, 0.75f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Left, true, false) loop 3, 4, 1f, 140),
        ))

        FallingBlockPattern.BottomRightCw -> MultiFallingBlockGenerator(listOf(
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Bottom, false, false) loop 2, 0, 0.5f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Bottom, false, false) loop 2, 2, 0.75f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Bottom, false, false) loop 3, 4, 1f, 140),
        ))

        FallingBlockPattern.BottomRightCcw -> MultiFallingBlockGenerator(listOf(
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Right, true, false) loop 2, 0, 0.5f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Right, true, false) loop 2, 2, 0.75f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Right, true, false) loop 3, 4, 1f, 140),
        ))

        FallingBlockPattern.TopLeftDualCw -> MultiFallingBlockGenerator(listOf(
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Top, false, true) loop 1, 0, 0.5f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Top, false, true) loop 1, 2, 0.75f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Top, false, true) loop 2, 4, 1f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Bottom, false, true) loop 1, 0, 0.5f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Bottom, false, true) loop 1, 2, 0.75f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Bottom, false, true) loop 2, 4, 1f, 140),
        ))

        FallingBlockPattern.TopLeftDualCcw -> MultiFallingBlockGenerator(listOf(
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Left, true, true) loop 1, 0, 0.5f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Left, true, true) loop 1, 2, 0.75f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Left, true, true) loop 2, 4, 1f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Right, true, true) loop 1, 0, 0.5f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Right, true, true) loop 1, 2, 0.75f, 140),
            SpiralFallingBlockGenerator(spiralStrategy(MatrixSide.Right, true, true) loop 2, 4, 1f, 140),
        ))
    }
    return generator
}