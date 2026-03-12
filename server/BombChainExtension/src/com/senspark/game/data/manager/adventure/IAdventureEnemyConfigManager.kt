package com.senspark.game.data.manager.adventure

import com.senspark.common.service.IServerService
import com.senspark.game.data.model.config.EnemyCreator
import com.senspark.game.data.model.config.Position
import com.senspark.game.data.model.user.AdventureEnemy

interface IAdventureEnemyConfigManager : IServerService {
    val enemyCreators: Map<Int, EnemyCreator>
    fun generateStoryEnemy(enemyId: Int, skin: Int): AdventureEnemy
    fun generateStoryEnemy(enemyId: Int, skin: Int, spawnPosition: Position): AdventureEnemy
    fun getBySkin(skin: Int): EnemyCreator
}