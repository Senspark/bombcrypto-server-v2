package com.senspark.game.pvp.entity

import com.senspark.game.pvp.utility.LongBitDecoder
import com.senspark.game.pvp.utility.LongBitEncoder

class HeroEffectState(
    override val isActive: Boolean,
    override val reason: HeroEffectReason,
    override val timestamp: Int,
    override val duration: Int,
) : IHeroEffectState {
    companion object {
        fun decode(state: Long): IHeroEffectState {
            val decoder = LongBitDecoder(state)
            return HeroEffectState(
                isActive = decoder.popBoolean(),
                reason = HeroEffectReason.values()[decoder.popInt(3)],
                timestamp = decoder.popInt(20),
                duration = decoder.popInt(16),
            )
        }
    }

    override fun encode(): Long {
        // 40 bits.
        val encoder = LongBitEncoder()
            .push(isActive)
            .push(reason.ordinal, 3)
            .push(timestamp, 20) // Upto 1000 seconds.
            .push(duration, 16) // Upto 60 seconds.
        return encoder.value
    }
}