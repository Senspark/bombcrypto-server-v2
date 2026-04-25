package com.senspark.game.utils

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AccountLevelHelperTest {

    @Test
    fun testGetAccountLevel() {
        assertEquals(1, AccountLevelHelper.getAccountLevel(0))
        assertEquals(1, AccountLevelHelper.getAccountLevel(149))
        assertEquals(2, AccountLevelHelper.getAccountLevel(150))
        assertEquals(2, AccountLevelHelper.getAccountLevel(329))
        assertEquals(3, AccountLevelHelper.getAccountLevel(330))
        assertEquals(10, AccountLevelHelper.getAccountLevel(3885))
        assertEquals(15, AccountLevelHelper.getAccountLevel(10762))
        assertEquals(20, AccountLevelHelper.getAccountLevel(50000))
    }

    @Test
    fun testGetBulkLimit() {
        assertEquals(1, AccountLevelHelper.getBulkLimit(1))
        assertEquals(5, AccountLevelHelper.getBulkLimit(5))
        assertEquals(10, AccountLevelHelper.getBulkLimit(10))
        assertEquals(15, AccountLevelHelper.getBulkLimit(15))
        assertEquals(15, AccountLevelHelper.getBulkLimit(20))
    }
}
