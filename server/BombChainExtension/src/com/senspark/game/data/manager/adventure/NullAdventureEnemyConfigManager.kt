package com.senspark.game.data.manager.adventure

import com.senspark.game.data.model.config.EnemyCreator
import com.senspark.game.data.model.config.Position
import com.senspark.game.data.model.user.AdventureEnemy
import com.senspark.game.exception.CustomException

class NullAdventureEnemyConfigManager : IAdventureEnemyConfigManager {

    override val enemyCreators: Map<Int, EnemyCreator> get() = emptyMap()

    override fun initialize() {
    }

    override fun generateStoryEnemy(enemyId: Int, skin: Int): AdventureEnemy {
        throw CustomException("Feature not support")
    }

    override fun generateStoryEnemy(enemyId: Int, skin: Int, spawnPosition: Position): AdventureEnemy {
        throw CustomException("Feature not support")
    }

    override fun getBySkin(skin: Int): EnemyCreator {
        throw CustomException("Feature not support")
    }
}