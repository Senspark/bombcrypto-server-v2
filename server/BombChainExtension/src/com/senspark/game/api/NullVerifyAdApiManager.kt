package com.senspark.game.api

class NullVerifyAdApiManager : IVerifyAdApiManager {

    override fun initialize() {
    }

    override suspend fun isValidAds(token: String?): Boolean {
        return true
    }

    override fun processAdsReward(json: String) {}
}