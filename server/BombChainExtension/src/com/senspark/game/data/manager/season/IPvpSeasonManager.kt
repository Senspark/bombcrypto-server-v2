package com.senspark.game.data.manager.season

import com.senspark.common.service.IServerService
import com.senspark.common.service.IService
import com.senspark.game.data.model.config.Season

interface IPvpSeasonManager : IService, IServerService {
    val currentSeason: Season
    val currentSeasonNumber: Int
    val currentRewardSeasonNumber: Int
} 