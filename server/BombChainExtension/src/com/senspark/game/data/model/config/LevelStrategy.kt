package com.senspark.game.data.model.config

import com.senspark.game.utils.deserialize
import com.senspark.game.utils.deserializeList
import com.senspark.lib.utils.Util
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.sql.ResultSet

class LevelStrategy(
    val stage: Int,
    val level: Int,
    val enemies: List<Int>,
    val enemiesNum: List<Int>,
    private val row: Int,
    private val col: Int,
    private val rowV1: Int,
    private val colV1: Int,
    val density: Double,
    val enemiesDoor: List<Int>,
    val enemiesDoorFirstNum: Int,
    val enemiesDoorThenNum: Int,
    val blocks: List<AdventureBlock>?,
    val playerSpawn: Position? = Position(0, 0),
    val door: Position?,
    val enemiesV2: List<AdventureMapConfigEnemy>?,
    val isFreeReviveHero: Boolean,
    val maxGoldReward: Int
) {
    companion object {
        fun fromResultSet(rs: ResultSet): LevelStrategy {
            return LevelStrategy(
                stage = rs.getInt("stage"),
                level = rs.getInt("level"),
                enemies = deserializeList(rs.getString("enemies")),
                enemiesNum = deserializeList(rs.getString("enemies_num")),
                row = rs.getInt("row"),
                col = rs.getInt("col"),
                rowV1 = rs.getInt("row_v1"),
                colV1 = rs.getInt("col_v1"),
                density = rs.getDouble("density"),
                enemiesDoor = deserializeList(rs.getString("enemies_door")),
                enemiesDoorFirstNum = rs.getInt("enemies_door_first_num"),
                enemiesDoorThenNum = rs.getInt("enemies_door_then_num"),
                blocks = if (rs.getString("blocks") != null) deserializeList(rs.getString("blocks")) else null,
                playerSpawn = if (rs.getString("player_spawn") != null) deserialize(rs.getString("player_spawn")) else null,
                door = if (rs.getString("door") != null) deserialize(rs.getString("door")) else null,
                enemiesV2 = if (rs.getString("enemies_v2") != null) deserializeList(rs.getString("enemies_v2")) else null,
                isFreeReviveHero = rs.getBoolean("is_free_revive_hero"),
                maxGoldReward = rs.getInt("max_gold_reward"),
            )
        }
    }

    fun getCol(version: Int): Int {
        return if (version == 1) colV1 else col
    }

    fun getRow(version: Int): Int {
        return if (version == 1) rowV1 else row
    }

    fun getEnemiesDoorNum(explodeCount: Int): Int {
        return if (explodeCount > 1) enemiesDoorThenNum else enemiesDoorFirstNum
    }
}

@Serializable
class AdventureBlock(
    val x: Int,
    val y: Int,
    val type: Int
) {
    constructor(x: Int, y: Int) : this(x, y, 0)

    fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putInt("i", x)
            putInt("j", y)
        }
    }
}


@Serializable
class AdventureMapConfigEnemy(

    @SerialName("rect_spawn")
    val rectSpawn: AdventureMapConfigEnemySpawn,
    @SerialName("enemy_id")
    val enemyId: Int
) {

    fun randomizeSpawnPosition(): Position {
        require(rectSpawn.width > 0) { "rectSpawn.width must > 0" }
        require(rectSpawn.height > 0) { "rectSpawn.height must > 0" }
        val deltaX = Util.randInt(0, rectSpawn.width - 1)
        val deltaY = Util.randInt(0, rectSpawn.height - 1)
        return Position(rectSpawn.x + deltaX, rectSpawn.y + deltaY)
    }
}

@Serializable
class AdventureMapConfigEnemySpawn(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)