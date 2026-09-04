package com.senspark.game.data.manager.nativeRate

/**
 * How far the native rate is allowed to move in one update.
 *
 * The rate is written from a third-party price feed, so a single bad quote is a question of when, not
 * if. Clamping rather than rejecting is deliberate: a genuine market move still arrives, just over a
 * few ticks, while a wrong one never lands whole and is undone by the next good quote. Rejecting would
 * do the opposite -- it holds the old price through exactly the swings worth tracking.
 *
 * Kept out of the scheduler so the rule can be tested without a server around it.
 */
object NativeRateStep {

    /**
     * The rate to store: [quoted], pulled no further than [maxChangePercent] from [current].
     *
     * A missing or unusable [current] means nothing to step from -- first write after a deploy, or a
     * network whose rate was never set -- so the quote is taken whole.
     */
    fun clamp(current: Double?, quoted: Double, maxChangePercent: Int): Double {
        require(quoted > 0.0) { "quoted rate must be positive, got $quoted" }
        require(maxChangePercent in 1..100) { "maxChangePercent out of range: $maxChangePercent" }
        if (current == null || current <= 0.0) {
            return quoted
        }
        val factor = maxChangePercent / 100.0
        return quoted.coerceIn(current * (1 - factor), current * (1 + factor))
    }
}
