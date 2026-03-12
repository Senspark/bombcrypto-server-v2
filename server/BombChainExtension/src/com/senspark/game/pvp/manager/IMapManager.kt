package com.senspark.game.pvp.manager

import com.senspark.game.pvp.entity.BlockReason
import com.senspark.game.pvp.entity.IBlock
import com.senspark.game.pvp.entity.IBlockState

interface IMapListener {
    fun onAdded(block: IBlock, reason: BlockReason)
    fun onRemoved(block: IBlock, reason: BlockReason)
}

interface IMapManagerState {
    val canDropChestBlock: Boolean

    /** States of blocks. */
    val blocks: Map<Pair<Int, Int>, IBlockState>

    fun apply(state: IMapManagerState): IMapManagerState
    fun encode(): List<Long>
}

/**
 * 1st layer: bombs/normal blocks.
 * 2nd layer: items.
 */
interface IMapManager {
    /** Gets the current state. */
    val state: IMapManagerState

    val tileset: Int
    val width: Int
    val height: Int
    val canDropChestBlock: Boolean

    fun applyState(state: IMapManagerState)

    /**
     * Gets the block at the specified position.
     * @param x Horizontal position.
     * @param y Vertical position.
     */
    fun getBlock(x: Int, y: Int): IBlock?

    /**
     * Adds the specified block to the map.
     * @param block The desired block.
     */
    fun addBlock(block: IBlock)

    /**
     * Removes the block at the specified position.
     * @param block The desired block.
     */
    fun removeBlock(block: IBlock)
}
