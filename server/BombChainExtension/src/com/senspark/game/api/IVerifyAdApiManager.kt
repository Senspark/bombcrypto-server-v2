package com.senspark.game.api

import com.senspark.common.service.IServerService

interface IVerifyAdApiManager : IServerService {
    suspend fun isValidAds(token: String?): Boolean
    fun processAdsReward(json: String)
}