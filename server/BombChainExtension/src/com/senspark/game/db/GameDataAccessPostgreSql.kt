package com.senspark.game.db

import com.google.gson.reflect.TypeToken
import com.senspark.common.IDatabase
import com.senspark.common.utils.ILogger
import com.senspark.game.controller.MapData
import com.senspark.game.data.PvPData
import com.senspark.game.data.PvPHeroEnergyData
import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.data.model.adventrue.UserAdventureMode
import com.senspark.game.data.model.config.OfflineRewardTHModeConfig
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.data.model.nft.House
import com.senspark.game.data.model.user.UserBlockReward
import com.senspark.game.db.helper.QueryHelper
import com.senspark.game.declare.EnumConstants.*
import com.senspark.game.schema.TableUserItemStatus
import com.senspark.game.utils.Extractor
import com.senspark.game.utils.Utils
import com.senspark.game.utils.deserialize
import com.senspark.game.utils.serialize
import com.senspark.lib.db.BaseDataAccess
import com.smartfoxserver.v2.entities.data.ISFSArray
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.Language
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Timestamp
import java.time.Instant
import java.util.*
import java.util.function.BiConsumer

class GameDataAccessPostgreSql(
    database: IDatabase,
    val log: Boolean,
    logger: ILogger,
) : BaseDataAccess(database, log, logger), IGameDataAccess {

    override fun initialize() {
    }

    /**
     * Lấy tất cả Hero Fi trong database
     */
    override fun getFiHeroes(uid: Int, dataType: DataType): ISFSArray {
        @Language("SQL")
        val statement = """
            SELECT ub.*, 1 AS status, ubl.lock_since, ubl.lock_seconds
            FROM "user_bomber" ub
            LEFT JOIN user_bomber_lock ubl ON ub.bomber_id = ubl.bomber_id AND ub.type = ubl.hero_type AND ub.data_type = ubl.data_type
            WHERE ub."uid" = ?
              AND ub."hasDelete" = 0
              AND ub.type in (?, ?, ?, ?, ?, ?)
              AND ub.data_type = ?;
        """.trimIndent()
        val params = arrayOf<Any?>(uid, HeroType.FI.value, HeroType.TON.value, HeroType.SOL.value, HeroType.RON.value, HeroType.BAS.value, HeroType.VIC.value, dataType.name)
        return executeQuery(statement, params)
    }

    /**
     * Lấy tất cả heroes thuộc loại nào đó trong database
     */
    override fun getFiHeroes(
        uid: Int,
        dataTypes: List<DataType>,
        lstBbmId: List<Int>,
        type: HeroType,
        listItemIds: List<Int>
    ): ISFSArray {
        val bomberIds = lstBbmId.joinToString(separator = ",")
//        @Language("SQL")
        val statement = """
            SELECT ub.*, ubl.lock_since, ubl.lock_seconds
            FROM "user_bomber" ub
            LEFT JOIN user_bomber_lock ubl ON ub.bomber_id = ubl.bomber_id AND ub.type = ubl.hero_type AND ub.data_type = ubl.data_type
            WHERE ub.uid = $uid
              AND ub."hasDelete" = 0
              AND ub.data_type IN (${dataTypes.joinToString(",") { "'${it.name}'" }})
              AND ${if (type != HeroType.FI) """"ub.type" = ${type.value} """ else "ub.bomber_id in ($bomberIds)"}
            """.trimIndent()
        return executeQuery(statement, arrayOf())
    }

    override fun getTonHeroes(uid: Int, limit: Int): ISFSArray {
        val statement = """
            SELECT * FROM fn_get_heroes_ton(?, ?);
        """.trimIndent()
        val params = arrayOf<Any?>(uid, limit)
        return executeQuery(statement, params)
    }

    override fun getHeroesOldSeason(uid: Int, type: HeroType): ISFSArray {
        val statement = """
            SELECT * FROM user_bomber_old_season WHERE uid = ? AND type = ?;
        """.trimIndent()
        val params = arrayOf<Any?>(uid, type.value)
        return executeQuery(statement, params)
    }

    override fun insertNewBomberman(
        userName: String,
        hero: Hero,
        dataType: DataType,
        itemId: Int
    ): InsertNewBombermanResult? {
        val pair = QueryHelper.queryInsertNewHero(hero, 1, dataType)
        if (hero.type == HeroType.TRIAL) {
            val statement2 = """UPDATE "user" set "type" = 1 WHERE id_user = ?"""
            val params2 = arrayOf<Any?>(hero.userId)
            val statement3 = """INSERT INTO log_user_trial(user_name, "type") 
                VALUES (?, ?)
            """.trimIndent()
            val params3 = arrayOf<Any?>(userName, "buy_hero")
            database.createQueryBuilder()
                .addStatementUpdate(pair.first, pair.second)
                //.addStatementUpdate(statement2, params2)
                .addStatementUpdate(statement3, params3)
                .executeUpdate()
        } else {
            val execute = database.createQueryBuilder().addStatementUpdate(pair.first, pair.second)
                .executeQuery()
            val returnVal = execute.getSFSObject(0)
            val bomberId = returnVal.getInt("bid")
            val lockUntil = Extractor.tryGet<Instant>(returnVal, "lock_until", Instant.MIN)
            val oldOwner = Extractor.tryGet<Int>(returnVal, "old_owner", -1)
            val isLocked = oldOwner > 0 || lockUntil > Instant.MIN
            if (hero.type == HeroType.TR) {
                transaction {
                    TableUserItemStatus.insert {
                        it[uid] = hero.userId
                        it[id] = bomberId
                        it[TableUserItemStatus.itemId] = itemId
                        it[status] = 0
                    }
                }
            }
            return InsertNewBombermanResult(bomberId, oldOwner, isLocked, lockUntil)
        }
        return null
    }

    override fun insertNewBomberman(
        uid: Int,
        dataType: DataType,
        heroes: List<Hero>
    ): Boolean {
        try {
            heroes.forEach { hero ->
                val pair = QueryHelper.queryInsertNewHero(hero, 1, dataType)
                database.createQueryBuilder().addStatementUpdate(pair.first, pair.second)
                    .executeQuery()
            }
            return true
        }
        catch (e: Exception) {
            return false
        }
    }

    override fun insertNewServerHero(
        userName: String,
        hero: Hero,
        dataType: DataType
    ): Int {
        // Prepare the SQL statement
        val statement = """
            SELECT * from fn_insert_new_server_hero (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) as new_bomber_id;
        """.trimIndent()
        val params = arrayOf<Any?>(
            hero.userId,
            hero.level,
            hero.bombPower,
            hero.bombRange,
            hero.stamina,
            hero.speed,
            hero.bombCount,
            hero.abilityList.toString(),
            hero.skin,
            hero.color,
            hero.rarity,
            hero.bombSkin,
            hero.shield.toString(),
            dataType.name,
            hero.abilityHeroSList.toString(),
            if (hero.isActive) 1 else 0,
            hero.type.value
        )
        val result = executeUpdateReturnValue(statement, params)
        if (result.size() == 0) {
            throw Exception("Insert new ton hero failed")
        }
        return result.getSFSObject(0).getInt("new_bomber_id")
    }

    override fun updateBombermanNotExist(uid: Int, dataType: DataType, heroIds: List<Int>) {
        if (heroIds.isEmpty()) {
            return
        }
        // sub query chưa thông tin cần cập nhật
        val subStatement = heroIds.joinToString("\n UNION \n") { e: Int ->
            "SELECT $uid AS uid, $e AS bomber_id".trimIndent()
        }
        val statement = """
            WITH new AS ($subStatement)
            UPDATE user_bomber AS ub
            SET "hasDelete" = 1
            FROM new
            WHERE ub.uid = new.uid
              AND ub.bomber_id = new.bomber_id
              AND ub.type = 0
              AND ub.data_type = ?;
        """.trimIndent()
        executeUpdate(statement, arrayOf(dataType.name))
    }

    override fun updateHeroDetails(uid: Int, dataType: DataType, hero: Hero): Boolean {
        val statement = """
            UPDATE user_bomber
            SET gen_id         = ?,
                level          = ?,
                uid            = ?,
                power          = ?,
                bomb_range     = ?,
                stamina        = ?,
                speed          = ?,
                bomb           = ?,
                bomb_skin      = ?,
                rare           = ?,
                charactor      = ?,
                ability        = ?,
                is_reset       = ?,
                ability_shield = ?,
                shield_level   = ?,
                shield         = ?,
                "hasDelete"    = 0,
                stake_amount   = ?,
                stage          = ?
            WHERE bomber_id = ?
              AND "type" = ?
              AND data_type = ?;
        """

        val params = arrayOf<Any?>(
            hero.details.details,
            hero.details.level,
            uid,
            hero.details.bombPower,
            hero.details.bombRange,
            hero.details.stamina,
            hero.details.speed,
            hero.details.bombCount,
            hero.details.bombSkin,
            hero.details.rarity,
            hero.details.skin,
            hero.details.abilityList.toString(),
            hero.details.resetShieldCounter,
            hero.details.abilityHeroSList.toString(),
            hero.details.shieldLevel,
            hero.shield.toString(),
            hero.stakeBcoin,
            hero.stage,
            hero.details.heroId,
            hero.type.value,
            dataType.name
        )
        return executeUpdate(statement, params)
    }
    

    override fun updateHeroDetails(
        uid: Int,
        dataType: DataType,
        heroes: List<Hero>
    ): Boolean {
        try {
            heroes.forEach { hero ->
                updateHeroDetails(uid, dataType, hero)
            }
            return true;
        }
        catch (e: Exception) {
            return false;
        }
        
    }
    

    override fun loadMapData(uid: Int, dataType: DataType): Map<MODE, MapData> {
        val statement = """
            SELECT *
            FROM "user_block_map_pve"
            WHERE "uid" = ?
              AND type = ?
        """.trimIndent()
        val resultMap: MutableMap<MODE, MapData> = EnumMap(MODE::class.java)
        executeQuery(statement, arrayOf(uid, dataType.name)) {
            val mapData = MapData()
            mapData.tileset = it.getInt("tileset")
            mapData.createdDate = it.getTimestamp("created_date").time
            mapData.createdDate = it.getTimestamp("updated_date").time
            mapData.mode = MODE.valueOf(it.getInt("mode"))
            val blockMap = it.getString("block_map")
            val blocksData = Json.decodeFromString<List<List<Int>>>(blockMap)
            mapData.setBlockFromJsonArray(blocksData)
            resultMap[MODE.valueOf(it.getInt("mode"))] = mapData
        }
        return resultMap
    }

    override fun loadSingleMapData(uid: Int, dataType: DataType, mode: MODE): MapData? {
        val statement = """
            SELECT *
            FROM "user_block_map_pve"
            WHERE "uid" = ?
              AND type = ?
              AND mode = ?;
        """.trimIndent()
        val listMapData = mutableListOf<MapData>()
        executeQuery(statement, arrayOf(uid, dataType.name, mode.value)) {
            val mapData = MapData()
            mapData.tileset = it.getInt("tileset")
            mapData.createdDate = it.getTimestamp("created_date").time
            mapData.createdDate = it.getTimestamp("updated_date").time
            mapData.mode = MODE.valueOf(it.getInt("mode"))
            val blockMap = it.getString("block_map")
            val blocksData = Json.decodeFromString<List<List<Int>>>(blockMap)
            mapData.setBlockFromJsonArray(blocksData)
            listMapData.add(mapData)
        }
        if (listMapData.isNotEmpty()) {
            return listMapData[0]
        }
        return null
    }

    override fun insertMapData(
        uid: Int,
        data: String,
        dataType: DataType,
        dateCreate: Long,
        tileset: Int,
        mode: MODE
    ): Boolean {
        val statement = """
            INSERT INTO "user_block_map_pve" ("uid", "mode", "type", "block_map", "tileset", "updated_date", "created_date")
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (uid, mode, type)
                DO UPDATE SET "block_map"    = ?,
                              "tileset"      = ?,
                              "updated_date" = ?,
                              "created_date" = ?;""".trimIndent()

        val createdTime = Timestamp(dateCreate)
        val updatedTime = Timestamp(System.currentTimeMillis())
        val params = arrayOf<Any?>(
            uid,
            mode.value,
            dataType.name,
            data,
            tileset,
            updatedTime,
            createdTime,
            data,
            tileset,
            updatedTime,
            createdTime
        )
        return executeUpdate(statement, params)
    }

    override fun updateMapData(uid: Int, mapData: MutableMap<MODE, MapData>, dataType: DataType): Boolean {
        val sub = mapData.values.joinToString("\n UNION \n") {
            """
                SELECT $uid AS uid,
                       ${it.mode.value} AS mode,
                       '${it.castBlocksToJsonArray()}' AS block_map
            """.trimIndent()
        }
        val statement = """
            UPDATE user_block_map_pve AS ubmp
            SET block_map    = new.block_map,
                updated_date = CURRENT_TIMESTAMP
            FROM (
                     $sub
                 ) AS new
            WHERE ubmp.uid = new.uid
              AND ubmp.mode = new.mode
              AND ubmp.type = ?
        """.trimIndent()
        return executeUpdate(statement, arrayOf(dataType.name))
    }

    override fun updateSingleMapData(uid: Int, mapData: MapData, dataType: DataType): Boolean {
        val statement = """
            UPDATE user_block_map_pve
            SET block_map = ?,
                updated_date = CURRENT_TIMESTAMP
            WHERE uid = ?
              AND mode = ?
              AND type = ?;
        """.trimIndent()
        val params = arrayOf<Any?>(
            mapData.castBlocksToJsonArray(),
            uid,
            mapData.mode.value,
            dataType.name
        )
        return executeUpdate(statement, params)
    }

    override fun loadUserAdventureModeController(uid: Int): UserAdventureMode? {
        val statement = """SELECT * FROM "user_adventure_mode" WHERE "uid" = ?"""
        val list = mutableListOf<UserAdventureMode>()
        executeQuery(statement, arrayOf(uid)) {
            list.add(UserAdventureMode.fromResulSet(it))
        }
        if (list.isNotEmpty()) {
            return list[0]
        }
        return null
    }

    override fun updateUserAdventureMode(
        uid: Int,
        userAdventureMode: UserAdventureMode
    ): Boolean {
        val statement = """
            INSERT INTO user_adventure_mode(uid, current_level, max_level, current_stage, max_stage, hero_id)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (uid) DO UPDATE
                SET current_level = excluded.current_level,
                    max_level     = excluded.max_level,
                    current_stage = excluded.current_stage,
                    max_stage     = excluded.max_stage,
                    hero_id       = excluded.hero_id;
        """.trimIndent()
        val params = arrayOf<Any?>(
            uid,
            userAdventureMode.currentLevel,
            userAdventureMode.maxLevel,
            userAdventureMode.currentStage,
            userAdventureMode.maxStage,
            userAdventureMode.heroId,
        )
        return executeUpdate(statement, params)
    }

    override fun loadUserBlockReward(
        uid: Int
    ): MutableMap<BLOCK_REWARD_TYPE, MutableMap<DataType, UserBlockReward>> {
        val statement = """
            SELECT *
            FROM "user_block_reward"
            WHERE "uid" = ?;
        """.trimIndent()
        val mapReward: MutableMap<BLOCK_REWARD_TYPE, MutableMap<DataType, UserBlockReward>> =
            EnumMap(BLOCK_REWARD_TYPE::class.java)

        executeQuery(statement, arrayOf(uid)) {
            val rewardType = BLOCK_REWARD_TYPE.valueOf(it.getString("reward_type"))
            val dataType = DataType.valueOf(it.getString("type"))
            val mapItem = mapReward[rewardType] ?: EnumMap(DataType::class.java)
            val reward = UserBlockReward(rewardType)
            val bigDecimalValue = BigDecimal(it.getDouble("values"))
            reward.values = bigDecimalValue.setScale(5, RoundingMode.DOWN).toFloat()
            reward.totalValues = it.getDouble("total_values")
            reward.lastTimeClaimSuccess = it.getTimestamp("last_time_claim_success").time
            reward.claimPending = it.getDouble("claim_pending")
            reward.dataType = dataType
            mapItem[dataType] = reward
            mapReward[rewardType] = mapItem
        }
        return mapReward
    }

    override fun subUserBlockReward(
        uid: Int,
        dataType: DataType,
        rewardType: BLOCK_REWARD_TYPE,
        value: Float,
        reason: String
    ) {
        val pair = QueryHelper.querySubUserBlockReward(uid, rewardType, value, dataType, reason)
        executeQueryAndThrowException(pair.first, pair.second) {}
    }

    override fun updateBomberEnergyAndStage(uid: Int, dataType: DataType, bombermans: List<Hero>): Boolean {
        if (bombermans.isEmpty()) {
            return true
        }
        // sub query chưa thông tin cần cập nhật
        val subStatement = bombermans.joinToString("\n UNION \n ") { e: Hero ->
            """
                    SELECT $uid AS uid, 
                    ${e.heroId} AS bomber_id, 
                    ${e.energy} AS energy, 
                    '${e.shield}' AS shield, 
                    ${e.stage} AS stage,
                    ${e.type.value} AS type,
                    ? AS time_rest
                """.trimIndent()
        }
        val args = bombermans.map { item: Hero -> Timestamp(item.timeRest) }.toTypedArray<Any?>()
        val statement = """
            WITH new AS ($subStatement)
            UPDATE user_bomber AS ub
            SET stage     = NEW.stage,
                time_rest = NEW.time_rest::timestamp,
                shield    = NEW.shield,
                energy    = NEW.energy
            FROM new
            WHERE ub.uid = new.uid
              AND ub.bomber_id = new.bomber_id
              AND ub.type = new.type
              AND ub.data_type = '${dataType.name}';
            """
        return executeUpdate(statement, args)
    }

    override fun updateBombermanActive(
        uid: Int,
        dataType: DataType,
        bomberId: Int,
        active: Boolean,
        stage: Int,
        type: Int,
        energy: Int
    ): Boolean {
        val statement = """
            UPDATE "user_bomber"
            SET "active"    = ?,
                "stage"     = ?,
                "time_rest" = ?,
                "energy"     = ?
            WHERE "uid" = ?
              AND "bomber_id" = ?
              AND "type" = ?
              AND data_type = ?;
            """.trimIndent()
        val timeRest = Timestamp(System.currentTimeMillis())
        val params = arrayOf<Any?>(if (active) 1 else 0, stage, timeRest, energy, uid, bomberId, type, dataType.name)
        return executeUpdate(statement, params)
    }

    override fun loadUserHouse(dataType: DataType, uid: Int): Map<Int, House> {
        val statement = """
            SELECT *, (EXTRACT(EPOCH FROM end_time_rent) * 1000)::BIGINT AS end_time_rent_convert
            FROM "user_house" 
            WHERE "uid" = ? AND type = ?;
            """.trimMargin()
        val mapHouse: MutableMap<Int, House> = HashMap()
        val params = arrayOf<Any?>(uid, dataType.name)
        executeQuery(statement, params) {
            val house = House.fromResultSet(it)
            mapHouse[house.houseId] = house
        }
        return mapHouse
    }

    override fun loadHeroInHouseRent(dataType: DataType, uid: Int): Map<Int, Int> {
        val statement = """
            SELECT * 
            FROM "user_hero_house_rent"
            WHERE is_rest = true AND house_id IN (
                SELECT house_id
                FROM user_house
                WHERE uid = ? AND type = ?
            );
            """.trimIndent()

        val mapHouse: MutableMap<Int, Int> = HashMap()
        val params = arrayOf<Any?>(uid, dataType.name)
        executeQuery(statement, params) {
            mapHouse[it.getInt("hero_id")] = it.getInt("house_id")
        }
        return mapHouse
    }

    override fun getAllHouseOldSeason(uid: Int, dataType: DataType): List<House> {
        val statement = """SELECT * FROM "user_house_old_season" WHERE "uid" = ? AND type = ?;"""
        val result = mutableListOf<House>()
        val params = arrayOf<Any?>(uid, dataType.name)
        executeQuery(statement, params) {
            val house = House.oldHouseFromResultSet(it)
            result.add(house)
        }
        return result
    }

    override fun updateUserHouseStage(dataType: DataType, uid: Int, houses: List<House>) {
        if (houses.isEmpty()) {
            return
        }
        val subQuery = houses.joinToString("\n UNION \n") { e: House ->
            """SELECT $uid AS uid, ${e.houseId} AS house_id, ${if (e.isActive) 1 else 0} AS active"""
        }
        val statement = """
            UPDATE user_house AS uh
            SET active = nh.active
            FROM ($subQuery) AS nh
            WHERE uh.house_id = nh.house_id
                AND uh.type = ?
        """.trimIndent()
        executeUpdate(statement, arrayOf(dataType.name))
    }

    override fun updateBombermanStage(
        uid: Int,
        bombermanList: List<Hero>,
        energiesRecovery: Map<Int, Int>
    ): Boolean {
        if (bombermanList.isEmpty()) {
            return true
        }
        val subQuery = bombermanList.joinToString("\n UNION \n") { e: Hero ->
            """SELECT $uid AS uid, ${e.heroId} AS bomber_id, ${e.stage} AS stage, ${energiesRecovery[e.heroId]} AS energy, ${e.type.value} AS type, CAST(? AS TIMESTAMP) AS time_rest"""
        }
        val args = bombermanList.map { item: Hero -> Timestamp(item.timeRest).toString() }.toTypedArray<Any?>()
        val statement = """
               UPDATE user_bomber
               SET stage = new.stage, time_rest = new.time_rest, energy = user_bomber.energy + new.energy
               FROM user_bomber  AS ub
               INNER JOIN ($subQuery) AS new ON ub.uid = new.uid AND ub.bomber_id = new.bomber_id
               AND ub.type = new.type
               WHERE user_bomber.bomber_id = ub.bomber_id and user_bomber.type = ub.type and user_bomber.uid = ub.uid;
               """.trimIndent()
        return executeUpdate(statement, args)
    }

    override fun updateBombermanName(name: String, bomberId: Int, userId: Int): Boolean {
        return try {
            val stmt = "UPDATE user_bomber SET name = ? WHERE bomber_id = ? AND uid = ?"
            val params = arrayOf<Any?>(name, bomberId, userId)
            executeUpdate(stmt, params)
        } catch (e: Exception) {
            false
        }
    }

    override fun deleteHouseNotExist(dataType: DataType, uid: Int, houseIds: List<Int>) {
        if (houseIds.isEmpty()) {
            return
        }
        val subQuery = houseIds.joinToString("\n UNION \n") { e: Int ->
            "SELECT $uid AS uid, $e AS house_id"
        }
        val statement = """
               DELETE
               FROM user_house
               WHERE (uid, house_id) IN ($subQuery)
                AND type = ?;
               """.trimIndent()
        executeUpdate(statement, arrayOf(dataType.name))
    }

    override fun insertNewHouse(dataType: DataType, uid: Int, house: House): Boolean {
        val statement = """
            INSERT INTO user_house(uid,
                                   gen_house_id,
                                   house_id,
                                   rarity,
                                   recovery,
                                   max_bomber,
                                   active,
                                   sync_date,
                                   type)
            VALUES (?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?,
                    ?)
            ON CONFLICT (house_id,type) DO UPDATE SET uid= excluded.uid,
                                                     gen_house_id = excluded.gen_house_id,
                                                     rarity = excluded.rarity,
                                                     recovery = excluded.recovery,
                                                     active = excluded.active,
                                                     max_bomber = excluded.max_bomber;
        """.trimIndent()
        val timeSync = Timestamp(System.currentTimeMillis())
        val params = arrayOf<Any?>(
            uid,
            house.details.details,
            house.houseId,
            house.rarity,
            house.recovery,
            house.capacity,
            if (house.isActive) 1 else 0,
            timeSync,
            dataType.name
        )
        return executeUpdate(statement, params)
    }

    override fun subUserDepositBcoin(
        uid: Int,
        dataType: DataType,
        depositRewardType: BLOCK_REWARD_TYPE,
        depositRewardAmount: Float,
        rewardType: BLOCK_REWARD_TYPE,
        rewardAmount: Float,
        reason: String
    ) {
        val builder = database.createQueryBuilder()
        if (depositRewardAmount > 0) {
            val pair = QueryHelper.querySubUserBlockReward(
                uid,
                depositRewardType,
                depositRewardAmount,
                dataType,
                reason
            )
            builder.addStatement(pair.first, pair.second)
        }
        if (rewardAmount > 0) {
            val pair = QueryHelper.querySubUserBlockReward(
                uid,
                rewardType,
                rewardAmount,
                dataType,
                reason
            )
            builder.addStatement(pair.first, pair.second)
        }
        builder.executeMultiQuery()
    }

    override fun queryUserRank(userId: Int): Int {
        val insert = "INSERT IGNORE INTO user_rank (uid) VALUES (?)"
        val insertParams = arrayOf<Any?>(userId)
        executeUpdate(insert, insertParams)
        val select = "SELECT * FROM user_rank WHERE uid = ?"
        val selectParams = arrayOf<Any?>(userId)
        val list = mutableListOf<Int>()
        executeQuery(select, selectParams) {
            list.add(it.getInt("points"))
        }
        return list[0]
    }


    override fun updateUserReward(
        uid: Int,
        dataType: DataType,
        listReward: Map<BLOCK_REWARD_TYPE, Float>?,
        otherWrappers: List<Pair<String, Array<Any?>>>?,
        reason: String
    ) {
        val builder = database.createQueryBuilder(false)
        if (!listReward.isNullOrEmpty()) {
            listReward.forEach(BiConsumer { k: BLOCK_REWARD_TYPE, v: Float ->
                val pair = QueryHelper.queryAddUpsertUserBlockReward(
                    uid,
                    k,
                    if (k.isTraditionalReward) DataType.TR else dataType,
                    v,
                    reason
                )
                builder.addStatement(pair.first, pair.second)
            })
        }
        if (!otherWrappers.isNullOrEmpty()) {
            otherWrappers.forEach {
                builder.addStatementUpdate(it.first, it.second)
            }
        }
        builder.executeMultiQuery()
    }

    override fun queryPvPHeroEnergy(userId: Int): List<PvPHeroEnergyData> {
        val statement = "INSERT INTO user_pvp_hero_energy(uid) VALUES (?) ON CONFLICT DO NOTHING"
        val statement1 = "SELECT * FROM user_pvp_hero_energy WHERE uid = ?"
        val params = arrayOf<Any?>(userId)
        executeUpdate(statement, params)
        val params1 = arrayOf<Any?>(userId)
        val list = mutableListOf<String>()
        executeQuery(statement1, params1) {
            list.add(it.getString("heroes"))
        }
        val heroesSFSObject = if (list.isNotEmpty()) list[0] else return ArrayList()
        val typeOfT = object : TypeToken<List<PvPHeroEnergyData?>?>() {}.type
        return Utils.jsonSerialize(heroesSFSObject, typeOfT) ?: ArrayList()
    }

    override fun queryPvP(userId: Int): PvPData {
        val statement = "INSERT INTO user_pvp (uid) VALUES (?) ON CONFLICT DO NOTHING"
        val statement1 = "SELECT * FROM user_pvp WHERE uid = ?"
        val params = arrayOf<Any?>(userId)
        executeUpdate(statement, params)
        val params1 = arrayOf<Any?>(userId)
        val listPvpData = mutableListOf<PvPData>()
        executeQuery(statement1, params1) {
            listPvpData.add(
                PvPData(
                    it.getLong("last_played_hero_id"),
                    it.getInt("last_bet")
                )
            )
        }
        return listPvpData[0]
    }

    override fun updatePvPHeroEnergy(userId: Int, heroes: String): Boolean {
        val statement = "UPDATE user_pvp_hero_energy SET heroes = ? WHERE uid = ?;"
        val params = arrayOf<Any?>(heroes, userId)
        return executeUpdate(statement, params)
    }

    override fun updateUserRankRewardClaim(userId: Int, season: Int, isClaim: Int): Pair<String, Array<Any?>> {
        val statement =
            "UPDATE user_pvp_rank_reward_ss_$season  SET is_claim = ? WHERE uid = ?"
        val params = arrayOf<Any?>(isClaim, userId)
        return Pair(statement, params)
    }

    override fun loadAllDisableFeatureConfigs(): Map<Int, IntArray> {
        val result = mutableMapOf<Int, IntArray>()
        val select = "SELECT * FROM game_disable_feature_config"
        executeQuery(select, emptyArray()) {
            result[it.getInt("version")] = deserialize(it.getString("disable_feature_ids"))
        }
        return result
    }

    override fun getHeroTraditional(
        uid: Int,
        configHeroTraditionalManager: IConfigHeroTraditionalManager
    ): ISFSArray {
        val itemIds = configHeroTraditionalManager.itemIds
        val statement = """
            SELECT ub.*,
                   ub.charactor                                            AS skin,
                   COALESCE(ubu.hp, 0)                                     AS upgraded_hp,
                   COALESCE(ubu.dmg, 0)                                    AS upgraded_dmg,
                   COALESCE(ubu.speed, 0)                                  AS upgraded_speed,
                   COALESCE(ubu.range, 0)                                  AS upgraded_range,
                   COALESCE(ubu.bomb, 0)                                   AS upgraded_bomb,
                   CASE WHEN ums.status IS NULL THEN 1 ELSE ums.status END AS status
            FROM "user_bomber" ub
                     LEFT JOIN user_item_status ums
                               ON ums.uid = ? AND ums.item_id IN (${itemIds.joinToString(",")}) AND
                                  ums.id = ub.bomber_id AND ub.type = 2
                     LEFT JOIN user_bomber_upgraded AS ubu ON ub.bomber_id = ubu.bomber_id AND ub.uid = ubu.uid
            WHERE ub."uid" = ?
              AND ub.type = 2
              AND ub."hasDelete" = 0
              AND (ums.status IS NULL OR ums.status <> 2);
        """.trimIndent()
        return executeQuery(statement, arrayOf(uid, uid))
    }

    override fun getOfflineRewardConfigs(): Map<Int, String> {
        val statement = """SELECT * FROM config_offline_reward"""
        val result = mutableMapOf<Int, String>()
        executeQueryAndThrowException(statement, emptyArray()) {
            result[it.getInt("offline_hours")] = it.getString("rewards_json")
        }
        return result
    }

    override fun getOfflineRewardTHModeConfigs(): MutableMap<DataType, OfflineRewardTHModeConfig> {
        val statement = "SELECT * FROM config_offline_reward_th_mode;"
        val resultHasMap: MutableMap<DataType, HashMap<String, String>> = mutableMapOf()
        executeQuery(statement, arrayOf()) {
            val key = it.getString("key")
            val value = it.getString("value")
            val type = it.getString("network")
            if (!resultHasMap.containsKey(DataType.valueOf(type))) {
                resultHasMap[DataType.valueOf(type)] = hashMapOf()
            }
            resultHasMap[DataType.valueOf(type)]!![key] = value
        }

        val result: MutableMap<DataType, OfflineRewardTHModeConfig> = mutableMapOf()
        resultHasMap.forEach {
            result[it.key] = OfflineRewardTHModeConfig(it.value)
        }
        return result
    }

    override fun updateBomberStakeAmount(
        dataType: DataType,
        bomberId: Int,
        type: Int,
        stakeBcoin: Double,
        stakeSen: Double
    ): Boolean {
        val statement = """
            UPDATE "user_bomber"
            SET "stake_amount" = ?,
                "stake_sen" = ?
            WHERE "bomber_id" = ?
              AND "type" = ?
              AND "data_type" = ?;
            """.trimIndent()
        val params = arrayOf<Any?>(stakeBcoin, stakeSen, bomberId, type, dataType.name)
        return executeUpdate(statement, params)
    }

    override fun addShieldToBomber(dataType: DataType, bomberId: Int, type: Int, shield: String): Boolean {
        val statement = """
            UPDATE "user_bomber"
            SET "shield" = ?,
                "ability_shield" = '[1]'
            WHERE "bomber_id" = ?
              AND "type" = ?
              AND "data_type" = ?;
            """.trimIndent()
        val params = arrayOf<Any?>(shield, bomberId, type, dataType.name)
        return executeUpdate(statement, params)
    }

    override fun getShieldHeroFromDatabase(dataType: DataType, bomberId: Int, type: Int): String {
        val statement = """
            SELECT shield
            FROM "user_bomber"
            WHERE "bomber_id" = ?
              AND "type" = ?
              AND "data_type" = ?;
            """.trimIndent()
        val selectParams = arrayOf<Any?>(bomberId, type, dataType.name)
        val list = mutableListOf<String>()
        executeQuery(statement, selectParams) {
            list.add(it.getString("shield"))
        }
        return list[0]
    }

    override fun logCreateRock(
        uid: Int,
        tx: String,
        listHeroId: List<Int>,
        amount: Float,
        network: DataType,
        status: String
    ): Boolean {
        val statement = """
            INSERT INTO user_create_rock(uid, tx, heroes, rock_amount, network, status)
            VALUES (?, ?, ?::jsonb, ?, ?, ?)
            ON CONFLICT (uid, tx) DO UPDATE
                SET status = excluded.status;
        """.trimIndent()
        val params = arrayOf<Any?>(uid, tx, listHeroId.serialize(), amount, network.name, status)
        return executeUpdate(statement, params)
    }

    override fun checkValidCreateRock(uid: Int, tx: String, network: DataType): Boolean {
        val statement = """
            SELECT *
            FROM "user_create_rock"
            WHERE "uid" = ?
              AND "tx" = ?
              AND "network" = ?
              AND ("status" = 'FALSE' OR "status" = 'DONE');
            """.trimIndent()
        val selectParams = arrayOf<Any?>(uid, tx, network.name)
        val sfsArray = executeQuery(statement, selectParams)
        return sfsArray.size() < 1
    }

    override fun updateStatusCreateRock(uid: Int, tx: String, network: DataType, status: String): Boolean {
        val statement = """
            UPDATE "user_create_rock"
            SET "status" = ?
            WHERE "uid" = ?
              AND "tx" = ?
              AND "network" = ?;
            """.trimIndent()
        val params = arrayOf<Any?>(status, uid, tx, network.name)
        return executeUpdate(statement, params)
    }

    override fun logSwapGem(
        uid: Int,
        tokenSwap: BLOCK_REWARD_TYPE,
        amount: Float,
        unitPrice: Float,
        network: DataType
    ): Boolean {
        val statement = """
            INSERT INTO user_swap_gem(uid, token_swap, amount, unit_price, network)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()
        val params = arrayOf<Any?>(uid, tokenSwap.name, amount, unitPrice, network.name)
        return executeUpdate(statement, params)
    }

    override fun checkValidSwapGem(uid: Int, timeSwapConfig: Int): Boolean {
        val statement = """
            SELECT *
            FROM "user_swap_gem"
            WHERE "uid" = ?
              AND DATE("time") = CURRENT_DATE;
            """.trimIndent()
        val selectParams = arrayOf<Any?>(uid)
        val sfsArray = executeQuery(statement, selectParams)
        return sfsArray.size() < timeSwapConfig
    }

    override fun updateRemainingTotalSwap(remainingTotal: Float): Boolean {
        val statement = """
            UPDATE "config_swap_token_realtime"
            SET "value" = ?
            WHERE "key" = 'remaining_total_dollar_swap'
            """.trimIndent()
        val params = arrayOf<Any?>(remainingTotal)
        return executeUpdate(statement, params)
    }

    override fun getHeroFiFromDatabase(dataType: DataType, bomberId: List<Int>, type: Int): ISFSArray {
        val statement = """
            SELECT *
            FROM "user_bomber"
            WHERE "bomber_id" IN (${bomberId.joinToString(",")})
              AND "type" = ?
              AND "data_type" = ?;
            """.trimIndent()
        val selectParams = arrayOf<Any?>(type, dataType.name)
        return executeQuery(statement, selectParams)
    }

    override fun getNextIdHouse(): Int {
        val statement = """
            SELECT NEXTVAL('user_house_id_seq') as id
        """.trimIndent()
        val params = arrayOf<Any?>()
        val list = mutableListOf<Long>()
        executeQuery(statement, params) {
            list.add(it.getLong("id"))
        }
        return list[0].toInt()
    }

    override fun getHouseOldSeason(uid: Int, houseId: Int, dataType: DataType): House? {
        val statement = """
            SELECT *
            FROM "user_house_old_season" 
            WHERE uid = ? AND house_id = ? AND type = ?;
        """.trimMargin()
        val list = mutableListOf<House>()
        val params = arrayOf<Any?>(uid, houseId, dataType.name)
        executeQuery(statement, params) {
            val house = House.oldHouseFromResultSet(it)
            list.add(house)
        }
        if (list.isNotEmpty()) {
            return list[0]
        }
        return null
    }

    override fun reactiveHouseOldSeason(uid: Int, houseId: Int, dataType: DataType) {
        val statement = """
            WITH moved_row AS (
                DELETE FROM user_house_old_season
                    WHERE uid = ? AND house_id = ? AND type = ?
                    RETURNING uid, gen_house_id, house_id, rarity, recovery, max_bomber, active, sync_date, type
            )
            INSERT INTO user_house (uid, gen_house_id, house_id, rarity, recovery, max_bomber, active, sync_date, type)
            SELECT uid, gen_house_id, house_id, rarity, recovery, max_bomber, 0, sync_date, type FROM moved_row;
            
            INSERT INTO logs.log_user_reactive_house (uid, house_id, data_type)
            VALUES (?, ?, ?)
        """.trimIndent()
        val params = arrayOf<Any?>(uid, houseId, dataType.name, uid, houseId, dataType.name)
        executeUpdate(statement, params)
    }

    override fun rentHouse(uid: Int, houseId: Int, dataType: DataType, endTime: Instant) {
        val statement = """
            UPDATE user_house
            SET end_time_rent = ?
            WHERE uid = ? AND house_id = ? AND type = ?
        """.trimIndent()
        val params = arrayOf<Any?>(Timestamp.from(endTime), uid, houseId, dataType.name)
        executeUpdate(statement, params)
    }

    override fun getUserQuantityHeroes(uid: Int, heroType: HeroType, dataType: DataType): Int {
        val statement = """
            SELECT count(*) as quantity
            FROM user_bomber
            WHERE uid = ? AND type = ? AND data_type = ? AND "hasDelete" = 0;
        """.trimMargin()
        val list = mutableListOf<Int>()
        val params = arrayOf<Any?>(uid, heroType.value, dataType.name)
        executeQuery(statement, params) {
            list.add(it.getInt("quantity"))
        }
        if (list.isNotEmpty()) {
            return list[0]
        }
        return 0
    }

    override fun heroRestHouseRent(heroId: Int, houseId: Int) {
        val statement = """
            INSERT INTO user_hero_house_rent (hero_id, house_id, is_rest)
            VALUES (?, ?, true)
            ON CONFLICT(hero_id, house_id) DO UPDATE SET is_rest = true;
        """.trimIndent()
        val params = arrayOf<Any?>(heroId, houseId)
        executeUpdate(statement, params)
    }

    override fun heroGoWorkFromHouseRent(heroId: Int, houseId: Int) {
        val statement = """
            UPDATE user_hero_house_rent
            SET is_rest = false
            WHERE hero_id = ? AND house_id = ?;
        """.trimIndent()
        val params = arrayOf<Any?>(heroId, houseId)
        executeUpdate(statement, params)
    }
}