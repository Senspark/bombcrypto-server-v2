package com.senspark.game.declare.customEnum

import com.senspark.game.declare.customEnum.ConfigUpgradeHeroType.DMG_HP
import com.senspark.game.declare.customEnum.ConfigUpgradeHeroType.SPEED_FIRE_BOMB

enum class ConfigUpgradeHeroType {
    DMG_HP,
    SPEED_FIRE_BOMB
}

enum class UpgradeHeroType(val type: ConfigUpgradeHeroType) {
    DMG(DMG_HP),
    HP(DMG_HP),
    RANGE(SPEED_FIRE_BOMB),
    SPEED(SPEED_FIRE_BOMB),
    BOMB(SPEED_FIRE_BOMB)
}