package com.senspark.game.data.model.config

import com.senspark.game.utils.deserialize
import com.senspark.game.utils.deserializeList
import java.sql.ResultSet

interface IAdventureMapConfig {
    val state: Int
    val level: Int
    val width: Int
    val height: Int
    val blocks: List<AdventureBlock>
    val playerSpawn: Position
    val door: Position
    val enemies: List<AdventureMapConfigEnemy>
}

class AdventureMapConfig(
    override val state: Int,
    override val level: Int,
    override val width: Int,
    override val height: Int,
    blocks: String,
    playerSpawn: String,
    door: String,
    enemies: String
) : IAdventureMapConfig {
    override val blocks: List<AdventureBlock>
    override val playerSpawn: Position
    override val door: Position
    override val enemies: List<AdventureMapConfigEnemy>

    init {
        this.blocks = deserializeList(blocks)
        this.playerSpawn = deserialize(playerSpawn)
        this.door = deserialize(door)
        this.enemies = deserializeList(enemies)
    }

    companion object {
        fun fromResultSet(rs: ResultSet): IAdventureMapConfig {
            return AdventureMapConfig(
                rs.getInt("state"),
                rs.getInt("level"),
                rs.getInt("width"),
                rs.getInt("height"),
                rs.getString("blocks"),
                rs.getString("player_spawn"),
                rs.getString("door"),
                rs.getString("enemies"),
            )
        }
    }
}