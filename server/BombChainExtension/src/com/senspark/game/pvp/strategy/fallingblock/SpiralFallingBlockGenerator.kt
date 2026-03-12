package com.senspark.game.pvp.strategy.fallingblock

import com.senspark.game.pvp.info.FallingBlockInfo
import com.senspark.game.pvp.info.IFallingBlockInfo
import com.senspark.game.pvp.strategy.matrix.*

class SpiralFallingBlockGenerator(
    private val _strategy: IMatrixStrategy,
    private val _offset: Int,
    private val _delayFraction: Float,
    private val _interval: Int,
) : IFallingBlockGenerator {
    init {
        require(_offset >= 0)
    }

    override fun generate(width: Int, height: Int, playTime: Int): List<IFallingBlockInfo> {
        // Assertion check amount of blocks.
        // val blockCount = (width - _offset * 2) * (height - _offset * 2) - // Outer.
        //     max(width - (_offset + _loop) * 2, 0) * max(height - (_offset + _loop) * 2, 0) // Inner.
        var state: IMatrixState = MatrixState(
            left = _offset,
            right = width - _offset - 1,
            top = _offset,
            bottom = height - _offset - 1,
            positions = emptyList()
        )
        state = _strategy.process(state)
        // Assertion check amount of blocks.
        // require(blockCount == state.positions.size)
        val delay = (playTime * _delayFraction).toInt()
        return state.positions.mapIndexed { index, (x, y) ->
            FallingBlockInfo(
                timestamp = delay + index * _interval,
                x = x,
                y = height - y - 1, // Reversed.
            )
        }
    }
}