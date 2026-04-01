package com.senspark.game.data.model.nft

import com.senspark.game.declare.EnumConstants
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.math.BigInteger

class BlockchainHeroDetailsTest {

    private fun buildDetailsString(resetShieldCounterValue: Int): String {
        // We need to place our 10-bit value at bit index 240.
        // We construct a BigInteger where the value is shifted by 240 bits.
        val value = BigInteger.valueOf(resetShieldCounterValue.toLong())
        val shifted = value.shiftLeft(240)
        return shifted.toString()
    }

    @Test
    fun testResetShieldCounterExtraction() {
        // Test case 1: 32 (binary: 100000)
        val detailsString32 = buildDetailsString(32)
        val hero32 = BlockchainHeroDetails(detailsString32, EnumConstants.DataType.BSC)
        assertEquals(32, hero32.resetShieldCounter)

        // Test case 2: 1023 (binary: 1111111111) - max for 10 bits
        val detailsString1023 = buildDetailsString(1023)
        val hero1023 = BlockchainHeroDetails(detailsString1023, EnumConstants.DataType.BSC)
        assertEquals(1023, hero1023.resetShieldCounter)
    }
}
