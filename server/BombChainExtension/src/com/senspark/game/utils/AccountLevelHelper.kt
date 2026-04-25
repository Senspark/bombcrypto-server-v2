package com.senspark.game.utils

object AccountLevelHelper {
    fun getAccountLevel(totalMinted: Int): Int {
        return when {
            totalMinted < 150 -> 1
            totalMinted < 330 -> 2
            totalMinted < 546 -> 3
            totalMinted < 805 -> 4
            totalMinted < 1116 -> 5
            totalMinted < 1489 -> 6
            totalMinted < 1936 -> 7
            totalMinted < 2473 -> 8
            totalMinted < 3115 -> 9
            totalMinted < 3885 -> 10
            totalMinted < 4809 -> 11
            totalMinted < 5918 -> 12
            totalMinted < 7249 -> 13
            totalMinted < 8846 -> 14
            totalMinted < 10762 -> 15
            // Hardcore levels (16-20) with 30% extra difficulty
            totalMinted < 14700 -> 16
            totalMinted < 20840 -> 17
            totalMinted < 30420 -> 18
            totalMinted < 45360 -> 19
            else -> 20
        }
    }

    fun getBulkLimit(level: Int): Int {
        return if (level >= 15) 15 else level
    }
}
