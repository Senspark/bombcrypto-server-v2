package com.senspark.game.data.manager.hero

import com.senspark.game.data.model.config.ResetShieldBomber
import com.senspark.game.exception.CustomException

class NullResetShieldBomberManager : IResetShieldBomberManager {

    override fun initialize() {
    }

    override fun getFinalDamage(rare: Int): Int {
        return 0
    }

    override fun get(rare: Int): ResetShieldBomber {
        throw CustomException("Feature not support")
    }

    override fun set(value: Map<Int, ResetShieldBomber>) {}
}