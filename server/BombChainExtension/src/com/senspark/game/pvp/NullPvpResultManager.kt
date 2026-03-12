package com.senspark.game.pvp

import com.senspark.game.api.IPvpResultInfo

class NullPvpResultManager : IPvpResultManager {

    override fun initialize() {
    }

    override fun claimReward(userId: Int): IPvpMatchReward? {
        return null
    }

    override fun handleResult(info: IPvpResultInfo) {}
}

