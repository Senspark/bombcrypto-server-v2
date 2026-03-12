package com.senspark.game.manager.pvp

import com.smartfoxserver.v2.entities.data.SFSObject

class NullUserPvpRankingManagerImpl : IUserPvpRankingManager {
    override fun claimReward(): SFSObject {
        return SFSObject()
    }
}