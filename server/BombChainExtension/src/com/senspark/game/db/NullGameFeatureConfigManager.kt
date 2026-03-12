package com.senspark.game.db

class NullGameFeatureConfigManager : IGameFeatureConfigManager {
    override fun initialize() {
    }

    override fun getDisableFeaturesByVersion(version: Int): List<Int> {
        return emptyList()
    }
}