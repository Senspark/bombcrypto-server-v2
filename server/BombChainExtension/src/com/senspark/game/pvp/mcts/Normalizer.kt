package com.senspark.game.pvp.mcts

import kotlin.math.abs
import kotlin.math.exp

class LinearNormalizer : INormalizer {
    override fun normalize(value: Float): Float {
        return value
    }
}

class ApproxSigmoidNormalizer : INormalizer {
    override fun normalize(value: Float): Float {
        return (value / (1f + abs(value)) + 1) * 0.5f
    }
}

class SigmoidNormalizer : INormalizer {
    override fun normalize(value: Float): Float {
        return 1f / (1f + exp(-value))
    }
}