package com.senspark.game.data

import com.senspark.game.data.model.adventrue.AdventureBlockItem
import com.senspark.game.data.model.config.AdventureBlock
import com.senspark.game.data.model.config.LevelStrategy
import com.senspark.game.data.model.config.Position
import com.senspark.game.data.model.user.AdventureEnemy

interface IAdventureMap {
    val blocks: Map<Int, Map<Int, AdventureBlock>>
    val door: Position
    val playerSpawn: Position
    val enemies: List<AdventureEnemy>
    val strategy: LevelStrategy
    val items: MutableMap<Int, MutableMap<Int, AdventureBlockItem>>
}

class AdventureMap(
    override val blocks: MutableMap<Int, MutableMap<Int, AdventureBlock>>,
    override val door: Position,
    override val playerSpawn: Position,
    override val enemies: List<AdventureEnemy>,
    override val strategy: LevelStrategy,
    override val items: MutableMap<Int, MutableMap<Int, AdventureBlockItem>>
) : IAdventureMap 