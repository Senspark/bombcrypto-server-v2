package com.senspark.game.data.manager.adventure

import com.senspark.game.data.model.config.EnemyCreator
import com.senspark.game.data.model.config.Position
import com.senspark.game.data.model.user.AdventureEnemy
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException

class AdventureEnemyConfigManager(
    private val _shopDataAccess: IShopDataAccess,
) : IAdventureEnemyConfigManager {

    override val enemyCreators: MutableMap<Int, EnemyCreator> = mutableMapOf()

    override fun initialize() {
        enemyCreators.putAll(_shopDataAccess.loadAdventureEnemyCreator())
    }

    override fun generateStoryEnemy(enemyId: Int, skin: Int): AdventureEnemy {
        return generateStoryEnemy(enemyId, skin, Position(0, 0))
    }

    override fun generateStoryEnemy(enemyId: Int, skin: Int, spawnPosition: Position): AdventureEnemy {
        val definition = enemyCreators[skin] ?: throw CustomException(
            "Cannot find enemy definition skin $skin",
            ErrorCode.SERVER_ERROR
        )
        return AdventureEnemy(
            enemyId,
            skin,
            definition.damage,
            definition.range,
            spawnPosition,
            definition.speedMove,
            definition.follow == 1,
            definition.health,
            definition.throughBrick == 1
        )
    }

    override fun getBySkin(skin: Int): EnemyCreator {
        return enemyCreators[skin] ?: throw CustomException("Cannot find enemy skin $skin")
    }
}