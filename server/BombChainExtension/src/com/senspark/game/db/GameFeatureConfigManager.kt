package com.senspark.game.db

private const val ALL_VERSION = 0

class GameFeatureConfigManager(
    private val _gameDataAccess: IGameDataAccess,
) : IGameFeatureConfigManager {

    private val _disableFeatureConfigs: MutableMap<Int, IntArray> = mutableMapOf()

    override fun initialize() {
        _disableFeatureConfigs.putAll(_gameDataAccess.loadAllDisableFeatureConfigs())
    }

    override fun getDisableFeaturesByVersion(version: Int): List<Int> {
        val lockFeaturesOfAllVersion = _disableFeatureConfigs[ALL_VERSION] ?: intArrayOf()
        val lockFeaturesOfVersion = _disableFeatureConfigs[version] ?: intArrayOf()
        return (lockFeaturesOfAllVersion + lockFeaturesOfVersion).distinct()
    }
}