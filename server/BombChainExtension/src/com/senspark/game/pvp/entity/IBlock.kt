package com.senspark.game.pvp.entity

enum class BlockReason {
    Null,

    /** Destroyed by bombs. */
    Exploded,

    /** Removed by falling blocks. */
    Removed,

    /** Consumed by hero. */
    Consumed,

    /** Spawn by map. */
    Spawn,

    /** Dropped by destroying a block. */
    Dropped,

    /** Falling block. */
    Falling,
}

enum class BlockType {
    Null,
    Hard,
    Soft,

    // Items.
    BombUp,
    FireUp,
    Boots,

    // Effects.
    Kick,
    Shield,
    Skull,

    // Rewards.
    GoldX1,
    GoldX5,
    BronzeChest,
    SilverChest,
    GoldChest,
    PlatinumChest,
}

val BlockType.isBlock get() = BlockType.Hard.ordinal <= ordinal && ordinal <= BlockType.Soft.ordinal
val BlockType.isItem get() = BlockType.BombUp.ordinal <= ordinal && ordinal <= BlockType.PlatinumChest.ordinal
val BlockType.isChestItem get() = BlockType.BronzeChest.ordinal <= ordinal && ordinal <= BlockType.PlatinumChest.ordinal

interface IBlock : IEntity {
    val state: IBlockState

    val reason: BlockReason

    /** Block type. */
    val type: BlockType

    /** Horizontal position. */
    val x: Int

    /** Vertical position. */
    val y: Int

    fun applyState(state: IBlockState)

    /**
     * Damages this block.
     * @param amount Amount of damage.
     */
    fun takeDamage(amount: Int)

    fun kill(reason: BlockReason)
}

val IBlock.isBlock get() = type.isBlock
val IBlock.isItem get() = type.isItem