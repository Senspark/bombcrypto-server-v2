package com.senspark.game.pvp.manager

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.delta.IBombStateDelta
import com.senspark.game.pvp.entity.*
import com.senspark.game.pvp.strategy.map.IExpandStrategy
import com.senspark.game.pvp.strategy.map.IExplodeRangeStrategy
import com.senspark.game.pvp.strategy.map.InstantExpandStrategy
import com.senspark.game.pvp.strategy.map.LinearExplodeRangeStrategy
import com.senspark.game.pvp.utility.LongBitEncoder

class BombManagerState(
    override val bombCounter: Int,
    override val bombs: Map<Int, IBombState>,
) : IBombManagerState {
    companion object {
        fun decodeDelta(delta: List<IBombStateDelta>): IBombManagerState {
            return BombManagerState(
                bombCounter = 0,
                bombs = delta.associate {
                    it.id to BombState.decode(it.state)
                },
            )
        }
    }

    override fun apply(state: IBombManagerState): IBombManagerState {
        val items = bombs.toMutableMap()
        state.bombs.forEach { (key, value) -> items[key] = value }
        return BombManagerState(
            bombCounter = state.bombCounter,
            bombs = items,
        )
    }

    override fun encode(): List<Long> {
        val baseEncoder = LongBitEncoder()
            .push(bombs.size, 10) // Upto 1000 bombs.
        return listOf(
            baseEncoder.value,
            *bombs.map { (item, state) ->
                val encodedState = state.encode()
                val encoder = LongBitEncoder()
                    .push(1 + encodedState.size, 5)
                    .push(item, 4)
                listOf(encoder.value, *encodedState.toTypedArray())
            }.flatten().toTypedArray(),
        )
    }
}

class DefaultBombManager(
    initialState: IBombManagerState,
    private val _logger: ILogger,
    private val _mapManager: IMapManager,
    private val _timeManager: ITimeManager,
    private val _listener: IBombListener,
) : IBombManager {
    private val _expandStrategy: IExpandStrategy = InstantExpandStrategy()
    private val _explodeRangeStrategy: IExplodeRangeStrategy = LinearExplodeRangeStrategy()
    private val _itemByPosition = mutableMapOf<Pair<Int, Int>, IBomb>()
    private val _itemById = mutableMapOf<Int, IBomb>()
    private val _itemsBySlot = mutableMapOf<Int, MutableList<IBomb>>() // Used for optimization.
    private val _deadItems = sortedMapOf<Int, IBomb>()
    private val _scheduledBombs = mutableListOf<Pair<Int, IBomb>>()
    private var _bombCounter = initialState.bombCounter

    override val state: IBombManagerState
        get() = BombManagerState(
            bombCounter = _bombCounter,
            bombs = (_itemById + _deadItems).mapValues { (_, bomb) -> bomb.state },
        )

    init {
        initialState.bombs.forEach { (id, state) ->
            val bomb = Bomb(
                id = id,
                initialState = state,
                _bombManager = this,
                _timeManager = _timeManager,
            )
            if (state.isAlive) {
                addBomb(bomb)
            } else {
                removeBomb(bomb)
            }
        }
    }

    override fun applyState(state: IBombManagerState) {
        state.bombs.forEach { (key, itemState) ->
            val item = Bomb(
                id = key,
                initialState = itemState,
                _bombManager = this,
                _timeManager = _timeManager,
            )
            if (itemState.isAlive) {
                val currentItem = _itemById[key]
                if (currentItem != null) {
                    currentItem.applyState(itemState)
                } else {
                    addBomb(item)
                }
            } else {
                val currentItem = _deadItems[key]
                if (currentItem != null) {
                    currentItem.applyState(itemState)
                } else {
                    if (_itemById.containsKey(item.id)) {
                        removeBomb(item)
                        if (item.reason == BombReason.Exploded) {
                            _listener.onExploded(item, item.state.explodeRanges)
                        }
                    } else {
                        // Removed since time-out (see step).
                    }
                }
            }
        }
    }

    override fun getBombs(slot: Int): List<IBomb> {
        return (_itemsBySlot[slot] ?: emptyList()) +
            _scheduledBombs
                .filter { it.second.slot == slot }
                .map { it.second }
    }

    override fun getBomb(x: Int, y: Int): IBomb? {
        return _itemByPosition[x to y]
    }

    override fun plantBomb(state: IBombState): IBomb {
        val bomb = Bomb(
            id = _bombCounter,
            initialState = state,
            _bombManager = this,
            _timeManager = _timeManager
        )
        addBomb(bomb)
        // Successful, apply changes to counter.
        ++_bombCounter
        return bomb
    }

    override fun addBomb(bomb: IBomb) {
        val x = bomb.x.toInt()
        val y = bomb.y.toInt()
        require(getBomb(x, y) == null) {
            "Bomb existed at x=$x y=$y"
        }
        _itemByPosition[x to y] = bomb
        _itemById[bomb.id] = bomb
        _itemsBySlot.getOrPut(bomb.slot) {
            mutableListOf()
        }.add(bomb)
        _deadItems.remove(bomb.id)
        _listener.onAdded(bomb, bomb.reason)
    }

    override fun removeBomb(bomb: IBomb) {
        val x = bomb.x.toInt()
        val y = bomb.y.toInt()
        _itemByPosition.remove(x to y)
        _itemById.remove(bomb.id)
        _itemsBySlot.getValue(bomb.slot).remove(bomb)
        _deadItems[bomb.id] = bomb
        _listener.onRemoved(bomb, bomb.reason)
    }

    override fun explodeBomb(bomb: IBomb) {
        val destroyedBlocks = mutableListOf<IBlock>()
        val expandResult = _expandStrategy.expand(this, _mapManager, bomb)
        expandResult.damagedPositions.forEach { (position, damage) ->
            val x = position.first
            val y = position.second
            // Dispatch events.
            _listener.onDamaged(x, y, damage) // Actual: damage hero.
            // Check damage block.
            val block = _mapManager.getBlock(x, y)
            if (block != null) {
                block.takeDamage(damage)
                if (!block.isAlive) {
                    destroyedBlocks.add(block)
                }
            }
        }
        expandResult.explodedBombs.forEach {
            it.kill(BombReason.Exploded)
            _listener.onExploded(it, it.state.explodeRanges)
        }
        assert(destroyedBlocks.size == setOf(destroyedBlocks).size)
        assert(destroyedBlocks.all { !it.isAlive })
        // Remove destroyed blocks.
        destroyedBlocks.forEach {
            it.kill(BlockReason.Exploded)
        }
    }

    override fun throwBomb(
        bomb: IBomb,
        direction: Direction,
        distance: Int,
        duration: Int,
    ) {
        // FIXME.
        val (newX, newY) = when (direction) {
            Direction.Left -> bomb.x - distance to bomb.y
            Direction.Right -> bomb.x + distance to bomb.y
            Direction.Up -> bomb.x to bomb.y + distance
            Direction.Down -> bomb.x to bomb.y - distance
        }
        if (newX.toInt() in 0 until _mapManager.width &&
            newY.toInt() in 0 until _mapManager.height) {
            // OK.
        } else {
            return
        }
        bomb.kill(BombReason.Removed)
        val timestamp = _timeManager.timestamp.toInt() + duration
        val state = bomb.state
        val scheduledBomb = Bomb(
            id = bomb.id,
            initialState = BombState(
                isAlive = state.isAlive,
                slot = state.slot,
                reason = BombReason.Planted,
                x = newX,
                y = newY,
                range = state.range,
                damage = state.damage,
                piercing = state.piercing,
                explodeDuration = state.explodeDuration + duration,
                explodeRanges = state.explodeRanges,
                plantTimestamp = state.plantTimestamp,
            ),
            _bombManager = this,
            _timeManager = _timeManager
        )
        _scheduledBombs.add(timestamp to scheduledBomb)
    }

    override fun getExplodeRanges(bomb: IBomb): Map<Direction, Int> {
        return listOf(
            Direction.Left,
            Direction.Right,
            Direction.Up,
            Direction.Down,
        ).associateWith {
            _explodeRangeStrategy.getExplodeRange(
                manager = _mapManager,
                x = bomb.x.toInt(),
                y = bomb.y.toInt(),
                range = bomb.range,
                piercing = bomb.piercing,
                direction = it,
            )
        }
    }

    override fun step(delta: Int) {
        // Remove old items.
        val maxAge = 60000 // 60 seconds.
        val thresholdTimestamp = _timeManager.timestamp - maxAge
        while (_deadItems.isNotEmpty()) {
            val key = _deadItems.firstKey()
            val item = _deadItems[key] ?: break
            if (item.plantTimestamp >= thresholdTimestamp) {
                break
            }
            _deadItems.remove(key)
        }
        // Clone to prevent java.util.ConcurrentModificationException.
        _itemByPosition.values.toList().forEach {
            it.update(delta)
        }
        _scheduledBombs.toList().forEach {
            val (timestamp, bomb) = it
            if (timestamp < _timeManager.timestamp) {
                _scheduledBombs.remove(it)
                try {
                    addBomb(bomb)
                } catch (ex: Exception) {
                    _logger.log(ex.stackTraceToString())
                }
            }
        }
    }
}