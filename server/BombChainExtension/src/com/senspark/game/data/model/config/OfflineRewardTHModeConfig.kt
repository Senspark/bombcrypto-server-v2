package com.senspark.game.data.model.config

import kotlinx.serialization.json.Json

class OfflineRewardTHModeConfig(
    var data: HashMap<String, String>,
) {
    companion object {
        private const val HEROES_DAME = "heroes_dame"
        private const val BLOCK_VALUE = "block_value"
        private const val BLOCK_HP = "block_hp"
        private const val NO_AUTO_MINE = "no_auto_mine"
        private const val ENERGY_USED = "energy_used"
    }
    val heroesDamage: List<Double> = Json.decodeFromString<List<Double>>(data[HEROES_DAME] ?: "[]")
    val blockHp: Double = data[BLOCK_HP]?.toDouble() ?: 0.0
    val blockValue: Double = data[BLOCK_VALUE]?.toDouble() ?: 0.0
    val noAutoMine: Double = data[NO_AUTO_MINE]?.toDouble() ?: 0.0
    val energyUsed: List<Double> = Json.decodeFromString<List<Double>>(data[ENERGY_USED] ?: "[]")
}