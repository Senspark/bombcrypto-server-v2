package com.senspark.game.pvp.manager

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.info.IFallingBlockInfo

interface IFallingBlockManagerListener {
    fun onBlockDidFall(x: Int, y: Int)
    fun onBuffered(blocks: List<IFallingBlockInfo>)
}

interface IFallingBlockManager : IUpdater

class FallingBlockManager(
    private val _blocks: List<IFallingBlockInfo>,
    private val _logger: ILogger,
    private val _listener: IFallingBlockManagerListener,
) : IFallingBlockManager {
    private var _elapsed = 0
    private var _index = 0
    private var _bufferIndex = 0

    override fun step(delta: Int) {
        _elapsed += delta
        // Process the next block.
        while (_index < _blocks.size) {
            val block = _blocks[_index]
            if (block.timestamp > _elapsed) {
                break
            }
            _listener.onBlockDidFall(block.x, block.y)
            ++_index
        }
        // Process buffer blocks.
        val bufferTime = 5000
        val bufferCount = 3
        while (_bufferIndex < _blocks.size) {
            val block = _blocks[_bufferIndex]
            if (block.timestamp > _elapsed + bufferTime) {
                break
            }
            val blocks = mutableListOf<IFallingBlockInfo>()
            for (i in 0 until bufferCount) {
                if (_bufferIndex >= _blocks.size) {
                    break
                }
                blocks.add(_blocks[_bufferIndex++])
            }
            _listener.onBuffered(blocks)
        }
    }
}