package com.senspark.game.manager.pvp

import com.smartfoxserver.v2.entities.data.SFSObject

interface IUserPvpRankingManager {
    fun claimReward(): SFSObject
}