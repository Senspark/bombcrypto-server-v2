package com.senspark.game.pvp.manager

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.delta.IBlockStateDelta
import com.senspark.game.pvp.entity.*
import com.senspark.game.pvp.info.IMapInfo
import com.senspark.game.pvp.utility.IRandom
import com.senspark.game.pvp.utility.LongBitDecoder
import com.senspark.game.pvp.utility.LongBitEncoder

class MapManagerState(
    override val canDropChestBlock: Boolean,
    override val blocks: Map<Pair<Int, Int>, IBlockState>,
) : IMapManagerState {
    companion object {
        fun create(
            info: IMapInfo,
            canDropChestBlock: Boolean,
        ): IMapManagerState {
            return MapManagerState(
                canDropChestBlock,
                info.blocks.associate {
                    require(it.type.isBlock) { "Invalid block type" }
                    (it.x to it.y) to BlockState(
                        isAlive = true,
                        reason = BlockReason.Spawn,
                        type = it.type,
                        health = it.health,
                        maxHealth = it.health,
                    )
                },
            )
        }

        fun decodeDelta(delta: List<IBlockStateDelta>): IMapManagerState {
            return MapManagerState(
                canDropChestBlock = false,
                blocks = delta.associate {
                    (it.x to it.y) to BlockState.decode(it.state)
                }
            )
        }

        fun decode(state: List<Long>): IMapManagerState {
            require(state.isNotEmpty()) { "Empty map state" }
            val baseDecoder = LongBitDecoder(state[0])
            val canDropChestBlock = baseDecoder.popBoolean()
            val blockSize = baseDecoder.popInt(10)
            val blocks = mutableMapOf<Pair<Int, Int>, IBlockState>()
            var index = 0
            while (index < state.size) {
                val decoder = LongBitDecoder(state[index])
                val size = decoder.popInt(5)
                val x = decoder.popInt(7)
                val y = decoder.popInt(7)
                val subState = BlockState.decode(state.subList(index + 1, index + size)[0])
                blocks[x to y] = subState
                index += size
            }
            require(blocks.size == blockSize) { "Invalid block size" }
            return MapManagerState(canDropChestBlock, blocks)
        }
    }

    override fun apply(state: IMapManagerState): IMapManagerState {
        val items = blocks.toMutableMap()
        state.blocks.forEach { (key, value) -> items[key] = value }
        return MapManagerState(
            canDropChestBlock = state.canDropChestBlock,
            blocks = items,
        )
    }

    override fun encode(): List<Long> {
        val baseEncoder = LongBitEncoder()
            .push(canDropChestBlock)
            .push(blocks.size, 10) // Upto 1000 blocks.
        return listOf(
            baseEncoder.value,
            *blocks.map { (key, state) ->
                val baseState = state.encode()
                val (x, y) = key
                val encoder = LongBitEncoder()
                    .push(1 + 1, 5)
                    .push(x, 7)
                    .push(y, 7)
                listOf(encoder.value, baseState)
            }.flatten().toTypedArray(),
        )
    }
}

class DefaultMapManager private constructor(
    override val tileset: Int,
    override val width: Int,
    override val height: Int,
    initialState: IMapManagerState,
    private val _logger: ILogger,
    private val _blockDropper: IBlockDropper,
    private val _timeManager: ITimeManager,
    private val _listener: IMapListener,
) : IMapManager {
    companion object {
        fun createMap(
            info: IMapInfo,
            logger: ILogger,
            timeManager: ITimeManager,
            random: IRandom,
            listener: IMapListener,
        ): IMapManager {
            return DefaultMapManager(
                tileset = info.tileset,
                width = info.width,
                height = info.height,
                initialState = MapManagerState.create(info, true),
                _logger = logger,
                _blockDropper = DefaultBlockDropper.create(info, logger, random),
                _timeManager = timeManager,
                _listener = listener,
            )
        }
    }

    /**
     * Maps from position to 1st layer block.
     * Bombs/normal blocks.
     */
    private val _items = mutableMapOf<Pair<Int, Int>, IBlock>()

    private val _deadItems = mutableMapOf<Pair<Int, Int>, IBlock>()

    private var _canDropItem: Boolean // Use to disable drop item when restoring state.
    private var _canDropChestBlock = initialState.canDropChestBlock

    override val state: IMapManagerState
        get() = MapManagerState(
            _canDropChestBlock,
            (_items + _deadItems).mapValues { (_, block) -> block.state },
        )

    override val canDropChestBlock get() = _canDropChestBlock

    init {
        _canDropItem = false
        initialState.blocks.forEach { (key, state) ->
            require(!state.type.isItem) { "Invalid normal block" }
            val (x, y) = key
            val item = Block(
                x = x,
                y = y,
                _initialState = state,
                _logger = _logger,
                _mapManager = this
            )
            if (state.isAlive) {
                addBlock(item)
            } else {
                removeBlock(item)
            }
        }
        _canDropItem = true
    }

    override fun applyState(state: IMapManagerState) {
        _canDropItem = false
        _canDropChestBlock = state.canDropChestBlock
        state.blocks.forEach { (key, itemState) ->
            val (x, y) = key
            val item = Block(
                x = x,
                y = y,
                _initialState = itemState,
                _logger = _logger,
                _mapManager = this,
            )
            if (itemState.isAlive) {
                val currentItem = _items[key]
                if (currentItem != null) {
                    if (currentItem.reason == itemState.reason) {
                        currentItem.applyState(itemState)
                        return@forEach
                    }
                    val reason = when (itemState.reason) {
                        BlockReason.Dropped -> BlockReason.Exploded
                        BlockReason.Falling -> BlockReason.Removed
                        else -> {
                            require(false) { "Invalid block reason" }
                            BlockReason.Null
                        }
                    }
                    currentItem.kill(reason)
                }
                addBlock(item)
            } else {
                val currentBlock = _deadItems[key]
                if (currentBlock != null) {
                    currentBlock.applyState(itemState)
                } else {
                    removeBlock(item)
                }
            }
        }
        _canDropItem = true
    }

    override fun getBlock(x: Int, y: Int): IBlock? {
        // Old map mechanics.
        if ((x % 2 == 1 && y % 2 == 1) ||
            (x < 0 || y < 0 || x >= width || y >= height)
        ) {
            // Hard block.
            // FIXME: lazy init block.
            return Block.createHardBlock(x, y, BlockReason.Null, _logger, this)
        }
        val position = x to y
        return _items[position]
    }

    override fun addBlock(block: IBlock) {
        require(getBlock(block.x, block.y) == null) {
            "Block exists at [${block.x} ${block.y}]"
        }
        _items[block.x to block.y] = block
        _deadItems.remove(block.x to block.y)
        _listener.onAdded(block, block.reason)
    }

    override fun removeBlock(block: IBlock) {
        _items.remove(block.x to block.y)
        _deadItems[block.x to block.y] = block
        _listener.onRemoved(block, block.reason)
        if (_canDropItem && block.isBlock && block.reason == BlockReason.Exploded) {
            val droppedBlock = _blockDropper.drop(this, block)
            if (droppedBlock != null) {
                addBlock(droppedBlock)
                if (droppedBlock.type.isChestItem) {
                    _canDropChestBlock = false
                }
            }
        }
    }
}
