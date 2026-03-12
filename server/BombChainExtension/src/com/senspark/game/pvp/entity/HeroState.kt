package com.senspark.game.pvp.entity

import com.senspark.game.pvp.utility.LongBitDecoder
import com.senspark.game.pvp.utility.LongBitEncoder

class HeroBaseState(
    override val isAlive: Boolean,
    override val health: Int,
    override val damageSource: HeroDamageSource,
    override val items: Map<HeroItem, Int>,
    override val effects: Map<HeroEffect, IHeroEffectState>,
) : IHeroBaseState {
    companion object {
        private val allItems = listOf(
            HeroItem.BombUp to 4, // 4 bits.
            HeroItem.FireUp to 4,
            HeroItem.Boots to 4,
            HeroItem.Gold to 7,
            HeroItem.BronzeChest to 1,
            HeroItem.SilverChest to 1,
            HeroItem.GoldChest to 1,
            HeroItem.PlatinumChest to 1,
        )
        private val allEffects = listOf(
            HeroEffect.Shield,
            HeroEffect.Invincible,
            HeroEffect.Imprisoned,
            HeroEffect.SpeedTo1,
            HeroEffect.SpeedTo10,
            HeroEffect.ReverseDirection,
            HeroEffect.PlantBombRepeatedly
        )

        fun decode(state: List<Long>): IHeroBaseState {
            val baseDecoder = LongBitDecoder(state[0])
            val itemDecoder = LongBitDecoder(state[1])
            val items = allItems.associate { (item, maxBits) ->
                item to itemDecoder.popInt(maxBits)
            }
            val effects = allEffects.withIndex().associate {
                it.value to HeroEffectState.decode(state[2 + it.index])
            }
            return HeroBaseState(
                isAlive = baseDecoder.popBoolean(),
                health = baseDecoder.popInt(4),
                damageSource = HeroDamageSource.values()[baseDecoder.popInt(3)],
                items = items,
                effects = effects,
            )
        }
    }

    override fun encode(): List<Long> {
        val items = listOf(
            // Base.
            LongBitEncoder()
                .push(isAlive)
                .push(health, 4)
                .push(damageSource.ordinal, 3)
                .value,
            // Items.
            LongBitEncoder().apply {
                allItems.forEach { (item, maxBits) ->
                    val value = items.getOrDefault(item, 0)
                    push(value, maxBits)
                }
            }.value,
            // Effects.
            *allEffects.map { effect ->
                val effectState = effects.getOrElse(effect) {
                    HeroEffectState(
                        isActive = false,
                        reason = HeroEffectReason.Null,
                        timestamp = 0,
                        duration = 0,
                    )
                }
                effectState.encode()
            }.toTypedArray()
        )
        return items
    }
}

class HeroPositionState(
    override val x: Float,
    override val y: Float,
    override val direction: Direction,
) : IHeroPositionState {
    companion object {
        private const val positionPrecision = 5

        fun decode(state: Long): IHeroPositionState {
            val decoder = LongBitDecoder(state)
            return HeroPositionState(
                x = decoder.popFloat(positionPrecision, 21),
                y = decoder.popFloat(positionPrecision, 21),
                direction = Direction.values()[decoder.popInt(2)],
            )
        }
    }

    override fun encode(): Long {
        val encoder = LongBitEncoder()
            .push(x, positionPrecision, 21)
            .push(y, positionPrecision, 21)
            .push(direction.ordinal, 2)
        return encoder.value
    }
}

class HeroState(
    override val baseState: IHeroBaseState?,
    override val positionState: IHeroPositionState?,
) : IHeroState {
    override val isAlive = baseState?.isAlive ?: false

    constructor(
        isAlive: Boolean,
        x: Float,
        y: Float,
        direction: Direction,
        health: Int,
        damageSource: HeroDamageSource,
        items: Map<HeroItem, Int>,
        effects: Map<HeroEffect, IHeroEffectState>
    ) : this(
        HeroBaseState(
            isAlive = isAlive,
            health = health,
            damageSource = damageSource,
            items = items,
            effects = effects,
        ),
        HeroPositionState(
            x = x,
            y = y,
            direction = direction,
        )
    )
}