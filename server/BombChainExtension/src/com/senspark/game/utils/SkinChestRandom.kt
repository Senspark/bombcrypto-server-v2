package com.senspark.game.utils

import com.senspark.game.data.SkinChestDropRateData
import kotlin.random.Random

class SkinChestRandom(dropRate: List<SkinChestDropRateData>) {
    private val _random = WeightedRandom(dropRate.last().dropRate)
    fun random(): Int {
        return _random.random(Random) + 1
    }
}