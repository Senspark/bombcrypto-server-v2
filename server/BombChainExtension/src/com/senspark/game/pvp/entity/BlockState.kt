package com.senspark.game.pvp.entity

import com.senspark.game.pvp.utility.LongBitDecoder
import com.senspark.game.pvp.utility.LongBitEncoder

open class BlockState(
    override val isAlive: Boolean,
    override val reason: BlockReason,
    override val type: BlockType,
    override val health: Int,
    override val maxHealth: Int,
) : IBlockState {
    companion object {
        fun decode(state: Long): IBlockState {
            val decoder = LongBitDecoder(state)
            return BlockState(
                type = BlockType.values()[decoder.popInt(5)],
                isAlive = decoder.popBoolean(),
                health = decoder.popInt(4),
                maxHealth = decoder.popInt(4),
                reason = BlockReason.values()[decoder.popInt(3)],
            )
        }
    }

    override fun encode(): Long {
        val encoder = LongBitEncoder()
            .push(type.ordinal, 5) // Prefer type first.
            .push(isAlive)
            .push(health, 4)
            .push(maxHealth, 4)
            .push(reason.ordinal, 3)
        return encoder.value
    }
}