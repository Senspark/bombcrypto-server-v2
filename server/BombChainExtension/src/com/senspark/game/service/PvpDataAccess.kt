package com.senspark.game.service

import com.senspark.common.IDatabase
import com.senspark.common.constant.PlayPvPMatchResult
import com.senspark.common.constant.PvPItemType
import com.senspark.common.data.IBombRank
import com.senspark.common.data.LogPlayPvPData
import com.senspark.common.pvp.IRankManager
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.DropRate
import com.senspark.game.constant.EventName
import com.senspark.game.constant.ItemType
import com.senspark.game.data.*
import com.senspark.game.data.model.user.BombRank
import com.senspark.game.data.model.user.PvPRank
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.exception.CustomException
import com.senspark.game.pvp.HandlerCommand
import com.senspark.game.pvp.entity.BlockType
import com.senspark.game.schema.*
import com.senspark.game.user.IUserInventoryManager
import com.senspark.game.utils.LogPlayPvPUserUtils
import com.senspark.game.utils.deserializeList
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.ResultSet
import java.time.Instant
import java.time.temporal.ChronoUnit

private fun Map<PvPItemType, Float>.toList(): List<Float> {
    val result = List(keys.max().value + 1) { 0f }.toMutableList()
    forEach { result[it.key.value] = it.value }
    return result
}

class PvpDataAccess(
    private val _db: IDatabase,
    private val _logLogger: ILogger,
    private val _sqlLogger: SqlLogger,
    private val _logEnable: Boolean,
    private val _statement: DatabaseStatement
) : IPvpDataAccess {

    private val _database = _db.database

    override fun initialize() {
    }

    override fun destroy() = Unit

    private fun String.executeQuery(vararg params: Any): ISFSArray {
        return try {
            _db.createQueryBuilder(_logEnable).addStatement(this, arrayOf(*params)).executeQuery()
        } catch (ex: Exception) {
            log(this, ex)
            SFSArray()
        }
    }

    private fun <T : Any> String.executeQuery(vararg params: Any, transform: (ResultSet) -> T) {
        try {
            _db.createQueryBuilder(_logEnable).addStatement(this, arrayOf(*params)).executeQuery(transform)
        } catch (ex: Exception) {
            log(this, ex)
            SFSArray()
        }
    }

    private fun String.executeUpdate(vararg params: Any) {
        try {
            _db.createQueryBuilder(_logEnable).addStatement(this, arrayOf(*params)).executeUpdate()
        } catch (ex: Exception) {
            log(this, ex)
        }
    }

    private fun log(statement: String, ex: Exception) {
        _logLogger.log("\n============DB_ERROR_LOG================\nstatement: $statement\nex${ex.message}\n============DB_ERROR_LOG================")
    }

    override fun queryEnableLogPvPCommands(): Set<String> {
        return setOf(
            HandlerCommand.FinishMatch,
            HandlerCommand.StartRound,
            HandlerCommand.FinishRound,
            HandlerCommand.MoveHero,
            HandlerCommand.PlantBomb,
            HandlerCommand.UseBooster,
            HandlerCommand.ObserverChangeState,
        )
    }

    override fun queryEvent(): Map<EventName, EventData> {
        val result = mutableMapOf<EventName, EventData>()
        _statement.queryEvent.executeQuery {
            result[EventName.fromValue(it.getString("name_event"))] = EventData(
                it.getLong("start_date"),
                it.getLong("end_date")
            )
        }
        return result
    }

    override fun queryRankingWithPoint(): List<IBombRank> {
        val result = mutableListOf<IBombRank>()
        _statement.queryPvpRankingConfig.executeQuery {
            result.add(BombRank.fromResultSet(it))
        }
        return result
    }

    override fun queryRankingWithPoint(currentPoint: Int): IBombRank {
        return queryRankingWithPoint().firstOrNull { it.startPoint <= currentPoint && currentPoint < it.endPoint }
            ?: throw CustomException("Ranking of $currentPoint not exists")
    }

    override fun queryLogPlayPvP(walletAddress: String): List<LogPlayPvPData> {
        val result = mutableListOf<LogPlayPvPData>()
        _statement.queryLogPlayPvP.executeQuery(
            walletAddress,
            walletAddress,
            walletAddress
        ) {
            result.add(
                LogPlayPvPData(
                    it.getInt("bet_value"),
                    it.getString("id_match"),
                    PlayPvPMatchResult.fromString(it.getString("match_result")),
                    it.getInt("time_match"),
                    Instant.ofEpochMilli(it.getLong("play_date")),
                    listOf(LogPlayPvPUserUtils.fromResultSet(it, 1), LogPlayPvPUserUtils.fromResultSet(it, 2)),
                )
            )
        }
        return result
    }

    override fun queryPvPBlockHealth(): List<PvPBlockHealthData> {
        return listOf(
            PvPBlockHealthData(BlockType.Hard, Int.MAX_VALUE),
            PvPBlockHealthData(BlockType.Soft, 1),
        )
    }

    private val _pvpBonusPoint = PvPBonusPointData(
        mapOf(
            0 to Pair(1, 3),
            1 to Pair(2, 3),
            2 to Pair(2, 4),
            3 to Pair(3, 4),
            4 to Pair(3, 5),
            5 to Pair(4, 5)
        )
    )

    override fun queryPvPBonusPoint(): PvPBonusPointData {
        return _pvpBonusPoint
    }

    override fun queryPvPChestDensity(): Float {
        return 0.05f
    }

    private val _pvpChestDropRate = mapOf(
        BlockType.BronzeChest to 0.5f,
        BlockType.SilverChest to 0.35f,
        BlockType.GoldChest to 0.1f,
        BlockType.PlatinumChest to 0.05f
    )

    override fun queryPvPChestDropRate(): Map<BlockType, Float> {
        return _pvpChestDropRate
    }

    private lateinit var _pvpItemDropRate: Map<BlockType, Float>

    override fun queryPvPItemDropRate(): Map<BlockType, Float> {
        if (!::_pvpItemDropRate.isInitialized) {
            _pvpItemDropRate = transaction {
                val execute = TableConfigDropRate.slice(TableConfigDropRate.dropRate)
                    .select { TableConfigDropRate.name eq DropRate.PVP_ITEM.name }
                    .first()[TableConfigDropRate.dropRate]
                val dropRate = deserializeList<Float>(execute)
                mapOf(
                    BlockType.GoldX1 to dropRate[0],
                    BlockType.GoldX5 to dropRate[1],
                    BlockType.Shield to dropRate[2],
                    BlockType.FireUp to dropRate[3],
                    BlockType.Boots to dropRate[4],
                    BlockType.BombUp to dropRate[5],
                    BlockType.Skull to dropRate[6]
                )
            }
        }
        return _pvpItemDropRate
    }

    private val _pvpMatchingPointDelta = listOf(
        PvPMatchingPointDeltaData(
            Pair(0, 100), Pair(0, 2000)
        ), PvPMatchingPointDeltaData(
            Pair(-100, 0), Pair(2000, 4000)
        ), PvPMatchingPointDeltaData(
            Pair(0, 300), Pair(4000, 6000)
        ), PvPMatchingPointDeltaData(
            Pair(-300, 0), Pair(6000, 8000)
        ), PvPMatchingPointDeltaData(
            Pair(0, 600), Pair(8000, 10000)
        ), PvPMatchingPointDeltaData(
            Pair(-600, 0), Pair(10000, 12000)
        ), PvPMatchingPointDeltaData(
            Pair(0, 1000), Pair(12000, 14000)
        ), PvPMatchingPointDeltaData(
            Pair(-1000, 0), Pair(14000, 16000)
        ), PvPMatchingPointDeltaData(
            Pair(Int.MIN_VALUE, Int.MAX_VALUE), Pair(16000, Int.MAX_VALUE)
        )
    )

    override fun queryPvPMatchingPointDelta(): List<PvPMatchingPointDeltaData> {
        return _pvpMatchingPointDelta
    }

    /**
     * TODO: so unoptimized
     */
    override fun queryPvPRank(season: Int, rankManager: IRankManager): MutableMap<Int, PvPRank> {
        val result = mutableMapOf<Int, PvPRank>()
        String.format(_statement.queryPvPRank, season).executeQuery {
            result[it.getInt("uid")] = PvPRank.fromResultSet(it, rankManager)
        }
        return result
    }

    override fun queryWinBonusPvPRankingPoint(default: Int, season: Int, userId: Int, dataType: DataType): Int {
        val point = mutableListOf<Int>()
        String.format(_statement.queryPvPRankingPoint, season).executeQuery(userId) {
            point.add(it.getInt("point"))
        }
        if (point.isEmpty()) {
            String.format(_statement.updatePvPRankingPoint, season, season).executeUpdate(
                userId,
                default,
                dataType.ordinal
            )
            return default
        }
        return point[0]
    }

    override fun queryVipBonusPvPRankingPoint(): List<VipWinBonusPvPRankingPointData> {
        return listOf(
            VipWinBonusPvPRankingPointData(1, true),
            VipWinBonusPvPRankingPointData(0, false),
        )
    }

    override fun queryPvPChestSpawnRadius(): Pair<Int, Int> {
        return Pair(3, 3)
    }

    private lateinit var _skinChestDropRate: List<SkinChestDropRateData>

    override fun querySkinChestDropRate(): List<SkinChestDropRateData> {
        if (!::_skinChestDropRate.isInitialized) {
            _skinChestDropRate = transaction {
                TableSkinChestDropRate.slice(TableSkinChestDropRate.value).selectAll().map {
                    SkinChestDropRateData(
                        deserializeList(it[TableSkinChestDropRate.value])
                    )
                }
            }
        }
        return _skinChestDropRate
    }

    override fun update(action: () -> Unit) {
        transaction(_database) {
            addLogger(_sqlLogger)
            action()
        }
    }

    override fun updateData(endTime: String, startTime: String) {
        String.format(_statement.updateData, startTime, endTime).executeUpdate()
    }

    /**
     * TODO: very unoptimized
     * Tính năng không sử dụng
     */
    override fun updateSkinChest(cost: Float, skinChest: OpenSkinChestData, userId: Int, userName: String) {
//        transaction(_database) {
//            addLogger(_logger)
//            val shardReward = TableUserReward.slice(TableUserReward.type, TableUserReward.value)
//                .select { (TableUserReward.userId eq userId) and (TableUserReward.type eq BLOCK_REWARD_TYPE.NFT_PVP.name) }
//                .firstOrNull()
//            val value = if (shardReward == null) 0f else shardReward[TableUserReward.value]
//            if (value < cost) {
//                throw Exception("Not enough shard")
//            }
//            TableUserReward.update({ (TableUserReward.userId eq userId) and (TableUserReward.type eq BLOCK_REWARD_TYPE.NFT_PVP.name) }) {
//                it[TableUserReward.value] = value - cost
//            }
//            TableUserSkin.insert {
//                it[itemId] = skinChest.itemId
//                it[type] = skinChest.type.value
//                it[uid] = userId
//            }
//        }
    }

    /**
     * TODO: very unoptimized
     */
    override fun updateSkinChest(id: Int, userId: Int, status: Int) {
        return transaction(_database) {
            addLogger(_sqlLogger)
            val skinChest = TableUserSkin
                .join(
                    TableUserItemStatus,
                    JoinType.LEFT,
                    additionalConstraint = {
                        (TableUserSkin.uid eq TableUserItemStatus.uid)
                            .and(TableUserSkin.itemId eq TableUserItemStatus.itemId)
                            .and(TableUserSkin.id eq TableUserItemStatus.id)
                    })
                .slice(
                    TableUserSkin.itemId,
                    TableUserSkin.id,
                    TableUserSkin.type,
                    TableUserSkin.uid,
                    TableUserItemStatus.status,
                    TableUserSkin.status,
                    TableUserSkin.expirationAfter,
                    TableUserItemStatus.expiryDate,
                )
                .select { (TableUserSkin.id eq id) and (TableUserSkin.uid eq userId) }
                .firstOrNull() ?: throw Exception("Could not find skin chest instant id: $id")
            if (skinChest[TableUserItemStatus.status] == TableUserItemStatus.STATUS_SELLING)
                throw Exception("Item's selling, could not equip")
            TableUserSkin.update({ (TableUserSkin.type eq skinChest[TableUserSkin.type]) and (TableUserSkin.uid eq userId) }) {
                it[TableUserSkin.status] = STATUS_INACTIVE
            }
            if (status == TableUserSkin.STATUS_ACTIVE) {
                TableUserSkin.update({ TableUserSkin.id eq id }) {
                    it[TableUserSkin.status] = status
                }
                if (skinChest[TableUserItemStatus.expiryDate] == null) {
                    val expirationAfter = skinChest[TableUserSkin.expirationAfter]
                        ?: IUserInventoryManager.DEFAULT_SKIN_ITEM_EXPIRY_TIME_IN_MILLIS
                    TableUserItemStatus.update({ (TableUserItemStatus.id eq id) and (TableUserItemStatus.itemId eq skinChest[TableUserSkin.itemId]) }) {
                        it[TableUserItemStatus.status] = STATUS_LOCK
                        if (expirationAfter > 0) {
                            it[expiryDate] = Instant.now().plus(expirationAfter, ChronoUnit.MILLIS)
                        }
                    }
                }
            }
        }
    }

    /**
     * TODO: very unoptimized
     */
    override fun updateSkinChest(userId: Int, itemType: ItemType, activeSkinIds: List<Int>) {
        return transaction(_database) {
            addLogger(_sqlLogger)
            val skins = TableUserSkin
                .join(
                    TableUserItemStatus,
                    JoinType.LEFT,
                    additionalConstraint = {
                        (TableUserSkin.uid eq TableUserItemStatus.uid)
                            .and(TableUserSkin.itemId eq TableUserItemStatus.itemId)
                            .and(TableUserSkin.id eq TableUserItemStatus.id)
                    })
                .slice(
                    TableUserSkin.itemId,
                    TableUserSkin.id,
                    TableUserSkin.type,
                    TableUserSkin.uid,
                    TableUserItemStatus.status,
                    TableUserSkin.status,
                    TableUserSkin.expirationAfter,
                    TableUserItemStatus.expiryDate,
                )
                .select { (TableUserSkin.id inList activeSkinIds) and (TableUserSkin.uid eq userId) }
                .toList()
            require(skins.size == activeSkinIds.size) { "userSkinIds contain invalid skin" }
            if (skins.any { it[TableUserItemStatus.status] == TableUserItemStatus.STATUS_SELLING })
                throw Exception("Contain selling item, could not equip")
            TableUserSkin.update({ (TableUserSkin.type eq itemType.value) and (TableUserSkin.uid eq userId) }) {
                it[status] = STATUS_INACTIVE
            }
            if (activeSkinIds.isNotEmpty()) {
                TableUserSkin.update({ TableUserSkin.id inList activeSkinIds }) {
                    it[status] = STATUS_ACTIVE
                }
                skins.forEach {
                    if (it[TableUserItemStatus.expiryDate] == null) {
                        val expirationAfter = it[TableUserSkin.expirationAfter]
                            ?: IUserInventoryManager.DEFAULT_SKIN_ITEM_EXPIRY_TIME_IN_MILLIS
                        TableUserItemStatus.update(
                            where = { (TableUserItemStatus.id eq it[TableUserSkin.id]) and (TableUserItemStatus.itemId eq it[TableUserSkin.itemId]) }
                        ) { it2 ->
                            it2[status] = STATUS_LOCK
                            if (expirationAfter > 0) {
                                it2[expiryDate] = Instant.now().plus(expirationAfter, ChronoUnit.MILLIS)
                            }
                        }
                    }
                }
            } else {
                TableUserSkin.update({ TableUserSkin.id inList activeSkinIds }) {
                    it[status] = STATUS_INACTIVE
                }
            }
        }
    }

    override fun updateUserReward(userId: Int, rewards: List<RewardData>, reason: String) {
        rewards.forEach { reward ->
            if (reward.value < 0) {
                // kiểm tra xong rồi thì đảo lại thành số dương để trừ
                val subValue = -reward.value
                _statement.subUserReward.executeQuery(
                    userId, reward.type, subValue,
                    reward.rewardType, reason
                ) {}
            } else if (reward.value > 0) {
                _statement.addUserReward.executeQuery(
                    userId, reward.type, reward.value,
                    reward.rewardType, reason
                ) {}
            }
        }
    }

    override fun updateUserRank(userId: Int, isWinner: Boolean, deltaPoint: Int, season: Int) {
        val tableName = "user_pvp_rank_ss_$season"
        val statement = """
            UPDATE $tableName
            SET total_match = total_match + 1,
                point = CASE WHEN point + ? < 0 THEN 0 ELSE point + ? END,
                win_match = win_match + ?,
                matches_in_current_date = matches_in_current_date + 1
            WHERE uid = ?
        """.trimIndent()

        val params = arrayOf<Any?>(deltaPoint, deltaPoint, if (isWinner) 1 else 0, userId)

        try {
            _db.createQueryBuilder()
                .addStatement(statement, params)
                .executeUpdate()
        } catch (e: Exception) {
            _logLogger.error("Error when updating user rank", e)
        }
    }

    override fun decayUserRank(season: Int, decayUsers: MutableMap<Int, Int>) {
        val subQuery = decayUsers.map {
            """SELECT ${it.key} AS uid, ${it.value} AS decay_point"""
        }.joinToString("\n UNION \n")
        val tableName = "user_pvp_rank_ss_$season"
        val statement = """
            UPDATE $tableName as uprs
            SET point = new.decay_point
            FROM ($subQuery) AS new 
            WHERE uprs.uid = new.uid;
            UPDATE $tableName
            SET matches_in_current_date = 0;
        """.trimIndent()
        statement.executeUpdate()
    }

    override fun getAmountPvpMatchesCurrentDate(userId: Int, season: Int): Int {
        val tableName = "user_pvp_rank_ss_$season"
        val statement = """
            SELECT matches_in_current_date
            FROM $tableName
            WHERE uid = $userId
        """.trimIndent()
        val list = mutableListOf<Int>()
        statement.executeQuery { list.add(it.getInt("matches_in_current_date")) }
        return if (list.isEmpty()) 0 else list[0]
    }

    override fun getAllAmountPvpMatchesCurrentDate(season: Int): Map<Int, Int> {
        val tableName = "user_pvp_rank_ss_$season"
        val statement = """
            SELECT uid, matches_in_current_date
            FROM $tableName
        """.trimIndent()
        val result = mutableMapOf<Int, Int>()
        statement.executeQuery { result[it.getInt("uid")] = it.getInt("matches_in_current_date")}
        return result
    }

    override fun getTotalPvpMatches(userId: Int): Int {
        var total = 0
        try {
            // Get all season tables
            val sqlTables = "SELECT table_name FROM information_schema.tables WHERE table_name LIKE 'user_pvp_rank_ss_%'"
            val tableNames = mutableListOf<String>()
            sqlTables.executeQuery { rs ->
                while (rs.next()) {
                    tableNames.add(rs.getString("table_name"))
                }
            }

            // Sum total_match from each table for this user
            for (table in tableNames) {
                val sqlCount = "SELECT total_match FROM $table WHERE uid = ?"
                _db.createQueryBuilder(false).addStatement(sqlCount, arrayOf(userId)).executeQuery { rs ->
                    if (rs.next()) {
                        total += rs.getInt("total_match")
                    }
                }
            }
        } catch (e: Exception) {
            _logLogger.error("Error calculating total pvp matches for user $userId", e)
        }
        return total
    }
}