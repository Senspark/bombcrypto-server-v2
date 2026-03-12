package com.senspark.game.pvp.entity

interface IBlockState : IEntityState {
    val reason: BlockReason

    /** Current block type. */
    val type: BlockType

    /** Current health. */
    val health: Int

    /** Maximum health. */
    val maxHealth: Int

    fun encode(): Long
}