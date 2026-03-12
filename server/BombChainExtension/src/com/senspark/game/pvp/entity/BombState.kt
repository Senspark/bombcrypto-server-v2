package com.senspark.game.pvp.entity

import com.senspark.game.pvp.utility.LongBitDecoder
import com.senspark.game.pvp.utility.LongBitEncoder

class BombState(
    override val isAlive: Boolean,
    override val slot: Int,
    override val reason: BombReason,
    override val x: Float,
    override val y: Float,
    override val range: Int,
    override val damage: Int,
    override val piercing: Boolean,
    override val explodeDuration: Int,
    override val explodeRanges: Map<Direction, Int>,
    override val plantTimestamp: Int,
) : IBombState {
    companion object {
        private const val positionPrecision = 5
        private val allDirections = listOf(
            Direction.Left to 4, // 4 bits.
            Direction.Right to 4,
            Direction.Up to 4,
            Direction.Down to 4,
        )

        fun decode(state: List<Long>): IBombState {
            val baseDecoder = LongBitDecoder(state[0])
            val auxDecoder = LongBitDecoder(state[1])
            return BombState(
                isAlive = baseDecoder.popBoolean(),
                x = baseDecoder.popFloat(positionPrecision, 21),
                y = baseDecoder.popFloat(positionPrecision, 21),
                explodeRanges = allDirections.associate { (item, maxBits) ->
                    item to baseDecoder.popInt(maxBits)
                },
                reason = BombReason.values()[baseDecoder.popInt(3)],
                slot = baseDecoder.popInt(2),
                plantTimestamp = auxDecoder.popInt(20),
                explodeDuration = auxDecoder.popInt(12),
                range = auxDecoder.popInt(4),
                damage = auxDecoder.popInt(4),
                piercing = auxDecoder.popBoolean(),
            )
        }
    }

    override fun encode(): List<Long> {
        val items = listOf(
            // Base.
            LongBitEncoder()
                .push(isAlive)
                .push(x, positionPrecision, 21)
                .push(y, positionPrecision, 21)
                .apply {
                    allDirections.forEach { (item, maxBits) ->
                        push(explodeRanges[item] ?: 0, maxBits)
                    }
                }
                .push(reason.ordinal, 3)
                .push(slot, 2)
                .value,
            // Aux.
            LongBitEncoder()
                .push(plantTimestamp, 20) // Upto 1000 seconds.
                .push(explodeDuration, 12) // Upto 4 seconds.
                .push(range, 4)
                .push(damage, 4)
                .push(piercing)
                .value,
        )
        return items
    }
}