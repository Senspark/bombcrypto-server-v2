package com.senspark.game.data.manager.season

import com.senspark.game.data.model.config.Season
import com.senspark.game.exception.CustomException

class NullPvpSeasonManager : IPvpSeasonManager {
    override val currentSeason: Season get() = throw CustomException("Feature not support")

    override val currentSeasonNumber: Int get() = 0

    override val currentRewardSeasonNumber: Int get() = 0

    override fun initialize() {
    }

    override fun destroy() = Unit
}