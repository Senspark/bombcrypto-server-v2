package com.senspark.game.data.manager.nativeRate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Passo máximo por atualização da taxa nativa ([NativeRateStep]). */
class NativeRateStepTest {

    private val tolerance = 1e-12

    @Test
    fun `takes the quote whole when there is no current rate`() {
        assertEquals(0.14, NativeRateStep.clamp(null, 0.14, 20), tolerance)
        assertEquals(0.14, NativeRateStep.clamp(0.0, 0.14, 20), tolerance)
        // A negative stored rate is nonsense, not a floor to step away from.
        assertEquals(0.14, NativeRateStep.clamp(-1.0, 0.14, 20), tolerance)
    }

    @Test
    fun `a move inside the band is applied exactly`() {
        assertEquals(0.11, NativeRateStep.clamp(0.10, 0.11, 20), tolerance)
        assertEquals(0.09, NativeRateStep.clamp(0.10, 0.09, 20), tolerance)
    }

    @Test
    fun `the band edge itself is not clamped`() {
        assertEquals(0.12, NativeRateStep.clamp(0.10, 0.12, 20), tolerance)
        assertEquals(0.08, NativeRateStep.clamp(0.10, 0.08, 20), tolerance)
    }

    @Test
    fun `a spike up is cut to the band`() {
        // Ten times the price is the shape of a broken quote; one tick may not deliver it.
        assertEquals(0.12, NativeRateStep.clamp(0.10, 1.0, 20), tolerance)
    }

    @Test
    fun `a collapse down is cut to the band`() {
        assertEquals(0.08, NativeRateStep.clamp(0.10, 0.0001, 20), tolerance)
    }

    @Test
    fun `a real move still converges over successive ticks`() {
        // The manual rate the migration ships with, against a market rate ~34% below it: the point of
        // clamping is that this arrives, just not in one jump.
        var rate = 0.214286
        val target = 0.140758
        repeat(3) { rate = NativeRateStep.clamp(rate, target, 20) }
        assertEquals(target, rate, tolerance)
    }

    @Test
    fun `each tick moves at most the configured percentage`() {
        val current = 0.10
        val applied = NativeRateStep.clamp(current, 5.0, 5)
        assertTrue(applied <= current * 1.05 + tolerance, "moved more than 5%: $applied")
    }

    @Test
    fun `refuses input that could only corrupt the stored rate`() {
        assertFailsWith<IllegalArgumentException> { NativeRateStep.clamp(0.10, 0.0, 20) }
        assertFailsWith<IllegalArgumentException> { NativeRateStep.clamp(0.10, -0.5, 20) }
        assertFailsWith<IllegalArgumentException> { NativeRateStep.clamp(0.10, 0.11, 0) }
        assertFailsWith<IllegalArgumentException> { NativeRateStep.clamp(0.10, 0.11, 101) }
    }
}
