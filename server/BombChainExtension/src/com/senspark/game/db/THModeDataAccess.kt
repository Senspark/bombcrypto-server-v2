package com.senspark.game.db

import com.senspark.common.IDatabase
import com.senspark.common.utils.ILogger
import com.senspark.game.data.model.config.*
import com.senspark.game.data.model.user.CoinRank
import com.senspark.game.data.model.user.ClubInfo
import com.senspark.game.data.model.user.UserClub
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.manager.treasureHuntV2.MultipleRewardResult
import com.senspark.game.manager.treasureHuntV2.UserId
import com.senspark.lib.db.BaseDataAccess
import org.intellij.lang.annotations.Language
import java.sql.Timestamp
import java.time.Instant
import kotlin.collections.HashMap

class THModeDataAccess(
    database: IDatabase,
    val log: Boolean,
    logger: ILogger,
) : BaseDataAccess(database, log, logger), ITHModeDataAccess {

    override fun initialize() {
    }

    override fun getNextRaceId(): Int {
        @Language("PostgreSQL")
        val statement = "SELECT nextval('logs.logs_th_mode_v2_seq_race_id')"
        val result = mutableListOf<Int>()
        executeQuery(statement, arrayOf()) { result.add(it.getLong("nextval").toInt()) }

        return result[0]
    }

    override fun loadTHModeV2Config(): Map<BLOCK_REWARD_TYPE, TreasureHuntV2Config> {
        val statement = "SELECT * FROM config_th_mode_v2;"
        val resultHasMap: MutableMap<BLOCK_REWARD_TYPE, HashMap<String, String>> = mutableMapOf()
        val rewardPool = loadRewardPoolConfig()

        executeQuery(statement, arrayOf()) {
            try {
                val key = it.getString("key")
                val value = it.getString("value")
                val type = it.getString("type")
                if (!resultHasMap.containsKey(BLOCK_REWARD_TYPE.valueOf(type))) {
                    resultHasMap[BLOCK_REWARD_TYPE.valueOf(type)] = hashMapOf()
                }
                resultHasMap[BLOCK_REWARD_TYPE.valueOf(type)]!![key] = value
            } catch (e: Exception) {
                // ignore
            }
        }

        val result: MutableMap<BLOCK_REWARD_TYPE, TreasureHuntV2Config> = mutableMapOf()
        resultHasMap.forEach {
            result[it.key] = TreasureHuntV2Config(it.value, rewardPool[it.key]!!)
        }
        return result
    }

    override fun updateTHModeV2Pool(newRewardPool: MutableMap<Int, Double>, type: BLOCK_REWARD_TYPE) {
        for ((poolId, remainingReward) in newRewardPool) {
            val statement = "UPDATE config_reward_pool_th_v2 SET remaining_reward = ? WHERE pool_id = ? and type = ?;"
            executeUpdate(statement, arrayOf(remainingReward, poolId, type.name), false)
        }
    }

    override fun refillTHModeV2Pool() {
        val statement = """
        UPDATE config_reward_pool_th_v2
        SET remaining_reward = max_reward;
        """.trimIndent()
        executeUpdate(statement, emptyArray(), false)
    }

    override fun loadMaxTHModeV2Pool(): Map<BLOCK_REWARD_TYPE, Map<Int, Double>> {
        val statement = "SELECT * FROM config_reward_pool_th_v2;"
        val result: MutableMap<BLOCK_REWARD_TYPE, MutableMap<Int, Double>> = mutableMapOf()
        executeQuery(statement, arrayOf()) {
            val poolId = it.getInt("pool_id")
            val maxReward = it.getInt("max_reward").toDouble()
            val type = BLOCK_REWARD_TYPE.valueOf(it.getString("type"))
            if (!result.containsKey(type)) {
                result[type] = mutableMapOf()
            }
            result[type]!![poolId] = maxReward
        }
        return result
    }

    override fun writeLogTHModeRewards(raceId: Int, data: Map<UserId, List<MultipleRewardResult>>) {
        val statement = """
            INSERT INTO logs.th_mode_v2 (race_id, uid, hero_id, network_id, pool_index, reward_level, timestamp, reward_bcoin, reward_sen, reward_coin)
            VALUES
            """.trimIndent()

        val str: StringBuilder = StringBuilder()
        for ((userId, rewardsByHero) in data) {
            for (rbh in rewardsByHero) {
                val list = rbh.reward
                val bcoin = list.find { it.blockRewardType == BLOCK_REWARD_TYPE.BCOIN }?.value ?: 0.0
                val sen = list.find { it.blockRewardType == BLOCK_REWARD_TYPE.SENSPARK }?.value ?: 0.0
                val coin = list.find { it.blockRewardType == BLOCK_REWARD_TYPE.COIN }?.value ?: 0.0
                val hero = rbh.hero
                val network = when (hero.details.dataType) {
                    DataType.BSC -> 0
                    DataType.POLYGON -> 1
                    DataType.TON -> 2
                    DataType.SOL -> 3
                    DataType.RON -> 4
                    DataType.BAS -> 5
                    else -> -1
                }
                str.appendLine("($raceId, ${userId.userId}, ${hero.heroId}, ${network}, ${rbh.hero.rarity}, ${rbh.rewardLevel}, CURRENT_TIMESTAMP, $bcoin, $sen, $coin),")
            }
        }
        if (str.isEmpty()) {
            return
        }

        // remove the last ',' character
        val output = str.removeRange(str.lastIndexOf(','), str.length)

        executeUpdate(statement + output, arrayOf(), false)
    }

    override fun loadRewardPoolConfig(): Map<BLOCK_REWARD_TYPE, MutableMap<Int, Double>> {
        val statement = "SELECT * FROM config_reward_pool_th_v2;"
        val result: MutableMap<BLOCK_REWARD_TYPE, MutableMap<Int, Double>> = mutableMapOf()
        executeQuery(statement, arrayOf()) {
            val poolId = it.getInt("pool_id")
            val remainingReward = it.getDouble("remaining_reward")
            val type = BLOCK_REWARD_TYPE.valueOf(it.getString("type"))
            if (!result.containsKey(type)) {
                result[type] = mutableMapOf()
            }
            result[type]!![poolId] = remainingReward
        }
        return result
    }

    override fun loadRewardLevelConfig(): Map<Int, RewardLevelConfig> {
        val statement = "SELECT * FROM config_reward_level_th_v2 ORDER BY level;"
        val result: MutableMap<Int, RewardLevelConfig> = mutableMapOf()
        executeQuery(statement, arrayOf()) {
            val level = it.getInt("level")
            result[level] = RewardLevelConfig.fromResultSet(it)
        }
        return result
    }

    override fun loadTreasureHuntDataConfig(): TreasureHuntDataConfig {
        val statement = "SELECT * FROM config_th_mode;"
        val sfsArray = executeQuery(statement, arrayOf())
        val result = TreasureHuntDataConfig.fromResultSet(sfsArray)
        return result
    }

    override fun saveRankingCoin(uid: Int, coin: Float, network: DataType, currentSeason: Int) {
        val statement = """
            INSERT INTO user_ranking_coin
            VALUES (?, ?, ?, ?)
            ON CONFLICT (uid, network, season) DO UPDATE
                SET coin = user_ranking_coin.coin + excluded.coin
            """
        executeUpdate(statement, arrayOf(uid, coin, network.name, currentSeason))
    }

    override fun getRankingCoin(currentSeason: Int, dataType: DataType): List<CoinRank> {
        val statement = if (dataType.isEthereumAirdropUser()) {
            """
            SELECT * FROM fn_get_coin_ranking_5(?, ?);
            """
        } else {
            """
            SELECT * FROM fn_get_coin_ranking_4(?, ?);
            """
        }

        val result = mutableListOf<CoinRank>()
        executeQuery(statement, arrayOf(currentSeason, dataType.name)) { result.add(CoinRank.fromResultSet(it)) }
        return result
    }

    override fun addNewAirdropSeason(newSeasonId: Int, timeStartNextSeason: Instant, timeEndNextSeason: Instant) {
        val statement = """
            INSERT INTO config_coin_ranking_season (id, start_date, end_date)
            VALUES (?, ?, ?)
            """
        executeUpdate(
            statement,
            arrayOf(newSeasonId, Timestamp.from(timeStartNextSeason), Timestamp.from(timeEndNextSeason))
        )
    }

    override fun logUserAirdropBuyActivity(uid: Int, itemIds: String, price: Float, type: String, dataType: DataType) {
        if (dataType == DataType.TON) {
            val statement = """
                INSERT INTO logs.log_user_ton_buy_activity
                VALUES (?, ?, ?, ?)
                """
            executeUpdate(statement, arrayOf(uid, itemIds, price, type))
        } else if (dataType == DataType.SOL) {
            val statement = """
            INSERT INTO logs.log_user_sol_buy_activity
            VALUES (?, ?, ?, ?)
            """
            executeUpdate(statement, arrayOf(uid, itemIds, price, type))
        }
        else if (dataType == DataType.RON) {
            val statement = """
            INSERT INTO logs.log_user_ron_buy_activity
            VALUES (?, ?, ?, ?)
            """
            executeUpdate(statement, arrayOf(uid, itemIds, price, type))
        }
        else if (dataType == DataType.BAS) {
            val statement = """
            INSERT INTO logs.log_user_bas_buy_activity
            VALUES (?, ?, ?, ?)
            """
            executeUpdate(statement, arrayOf(uid, itemIds, price, type))
        }
        else if (dataType == DataType.VIC) {
            val statement = """
            INSERT INTO logs.log_user_vic_buy_activity
            VALUES (?, ?, ?, ?)
            """
            executeUpdate(statement, arrayOf(uid, itemIds, price, type))
        }
    }

    override fun logOfflineReward(
        uid: Int,
        timeLogOut: Instant,
        timeOffline: Double,
        reward: Double,
        dataType: DataType
    ) {
        val statement = """
            INSERT INTO logs.log_user_receive_offline_reward (uid, log_out, time_offline, reward, network)
            VALUES (?, ?, ?, ?, ?)
            """
        executeUpdate(statement, arrayOf(uid, Timestamp.from(timeLogOut), timeOffline, reward, dataType.name))
    }

    override fun getTasksConfig(): MutableMap<Int, TonTasksConfig> {
        val statement = "SELECT id, type, reward FROM config_ton_tasks where deleted = 0"
        val result = mutableMapOf<Int, TonTasksConfig>()
        executeQuery(statement, arrayOf()) {
            result[it.getInt("id")] = TonTasksConfig.fromResultSet(it)
        }
        return result
    }

    override fun saveTaskComplete(uid: Int, taskId: Int) {
        val statement = """
            INSERT INTO user_ton_completed_tasks (uid, task_id)
            VALUES (?, ?);
            """
        executeUpdate(statement, arrayOf(uid, taskId))
    }

    override fun chaimTask(uid: Int, taskId: Int, reward: Double) {
        val statement = """
            CALL sp_claim_ton_tasks(?, ?, ?);
            """.trimIndent()
        val params = arrayOf<Any?>(uid, taskId, reward)
        executeUpdate(statement, params)
    }

    override fun getUserCompletedTasks(uid: Int): MutableMap<Int, Boolean> {
        val statement = """
            SELECT task_id, claimed
            FROM "user_ton_completed_tasks"
            WHERE "uid" = ?
            """.trimIndent()
        val result = mutableMapOf<Int, Boolean>()
        executeQuery(statement, arrayOf(uid)) {
            val taskId = it.getInt("task_id")
            val isClaimed = it.getInt("claimed") == 1
            result[taskId] = isClaimed
        }
        return result
    }

    override fun logUserFusionHeroServer(
        uid: Int,
        heroIds: List<Int>,
        result: String,
        reasonFail: String,
        amountTonNeedToFusion: Double,
        dataType: DataType
    ) {
        if (dataType == DataType.TON) {
            val statement = """
                INSERT INTO logs.log_user_ton_fusion_v2 (uid, hero_ids, fee, results, reason_fail, create_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """.trimIndent()

            val heroIdsString = heroIds.joinToString(",")

            executeUpdate(statement, arrayOf(uid, heroIdsString, amountTonNeedToFusion, result, reasonFail))
        } else if (dataType == DataType.SOL) {
            val statement = """
                INSERT INTO logs.log_user_sol_fusion_v2 (uid, hero_ids, fee, result, reason_fail, create_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """.trimIndent()

            val heroIdsString = heroIds.joinToString(",")
            executeUpdate(statement, arrayOf(uid, heroIdsString, amountTonNeedToFusion, result, reasonFail))
        }
        else if (dataType == DataType.RON) {
            val statement = """
                INSERT INTO logs.log_user_ron_fusion (uid, hero_ids, fee, result, reason_fail, create_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """.trimIndent()

            val heroIdsString = heroIds.joinToString(",")
            executeUpdate(statement, arrayOf(uid, heroIdsString, amountTonNeedToFusion, result, reasonFail))
        }
        else if (dataType == DataType.BAS) {
            val statement = """
                INSERT INTO logs.log_user_bas_fusion (uid, hero_ids, fee, result, reason_fail, create_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """.trimIndent()

            val heroIdsString = heroIds.joinToString(",")
            executeUpdate(statement, arrayOf(uid, heroIdsString, amountTonNeedToFusion, result, reasonFail))
        }
        else if (dataType == DataType.VIC) {
            val statement = """
                INSERT INTO logs.log_user_vic_fusion (uid, hero_ids, fee, result, reason_fail, create_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """.trimIndent()

            val heroIdsString = heroIds.joinToString(",")
            executeUpdate(statement, arrayOf(uid, heroIdsString, amountTonNeedToFusion, result, reasonFail))
        }
    }

    override fun logClaimReferral(uid: Int, amount: Float) {
        val statement = """
            INSERT INTO logs.log_user_claim_referral (uid, amount)
            VALUES (?, ?)
            """
        executeUpdate(statement, arrayOf(uid, amount))
    }

    override fun getReferralParamsConfig(): MutableMap<String, Int> {
        val statement = """
            SELECT *
            FROM "config_referral_params"
            """
        val result = mutableMapOf<String, Int>()
        executeQuery(statement, arrayOf()) {
            result[it.getString("name")] = it.getInt("id")
        }
        return result
    }

    override fun getCoinLeaderboardConfig(): List<CoinLeaderboardConfig> {
        val statement = """
        SELECT *
        FROM "config_coin_leaderboard"
        """
        val result = mutableListOf<CoinLeaderboardConfig>()
        executeQuery(statement, arrayOf()) {
            result.add(CoinLeaderboardConfig.fromResultSet(it))
        }
        return result
    }

    override fun getAllClub(season: Int): MutableList<ClubInfo> {
        val statement = """
            WITH result AS (SELECT club_id,
                                   SUM(point) AS point_total,
                                   SUM(CASE WHEN season = ? THEN point ELSE 0 END) AS point_current_season
                            FROM user_club_point_v2
                            GROUP BY club_id)
            SELECT r.*, uc.name, uc.link, uc.avatar_name, uc.id_telegram
            FROM result r JOIN user_club_v2 uc ON r.club_id = uc.id;
            """
        val result = mutableListOf<ClubInfo>()
        executeQuery(statement, arrayOf(season)) {
            result.add(ClubInfo.fromResultSet(it))
        }
        return result
    }

    // dùng để cho bot insert do tạo club bên telegram sẽ có idTelegram
    override fun addNewClub(idTelegram: Long, name: String, link: String, season: Int, avatarName: String): Int {
        // Insert into user_club_v2 and get generated id
        val clubType = EnumConstants.ClubType.TELEGRAM.name
        val statement = """
            INSERT INTO user_club_v2 (id_telegram, name, link, avatar_name, type)
            VALUES (?, ?, ?, ?, ?) RETURNING id;
            """
        val result = mutableListOf<Int>()
        executeQuery(statement, arrayOf(idTelegram, name, link, avatarName, clubType)) {
            result.add(it.getInt("id"))
        }
        val clubId = result.firstOrNull() ?: throw Exception("Failed to insert club")

        // Insert into user_club_point
        val pointStatement = "INSERT INTO user_club_point_v2 (club_id, season) VALUES (?, ?);"
        executeUpdate(pointStatement, arrayOf(clubId, season))

        return clubId
    }

    // Dùng để cho client gọi tạo club, client tạo thì ko có idTelegram
    override fun addNewClubV2(name: String, season: Int, type: EnumConstants.ClubType): Int {
        // Insert into user_club and get generated id
        val statement = """
            INSERT INTO user_club_v2 (name, type)
            VALUES (?, ?) RETURNING id;
        """
        val result = mutableListOf<Int>()
        executeQuery(statement, arrayOf(name, type.name)) {
            result.add(it.getInt("id"))
        }
        val clubId = result.firstOrNull() ?: throw Exception("Failed to insert club")
        // Insert into user_club_point
        val pointStatement = "INSERT INTO user_club_point_v2 (club_id, season) VALUES (?, ?);"
        executeUpdate(pointStatement, arrayOf(clubId, season))
        return clubId
    }

    override fun getUserClubs(season: Int): MutableMap<Int, UserClub> {
        val statement = """
            WITH result AS (SELECT uid,
                                   club_id,
                                   SUM(point) AS point_total,
                                   SUM(CASE WHEN season = ? THEN point ELSE 0 END) AS point_current_season
                                   FROM "user_club_members_v2"
                                   WHERE is_leave = 0
                                   GROUP BY uid, club_id)
            SELECT r.*,
                   CASE
                       WHEN u.name IS NOT NULL THEN u.name
                       WHEN u.second_username IS NOT NULL THEN u.second_username
                       ELSE
                           CASE
                               WHEN LENGTH(u.user_name) > 10 THEN CONCAT(SUBSTRING(u.user_name, 0, 6), '...',
                                                                         SUBSTRING(u.user_name, LENGTH(u.user_name) - 3, 4))
                               ELSE u.user_name END
                       END as name
            FROM result AS r
                     INNER JOIN "user" AS u ON r.uid = u.id_user;
            """
        val result = mutableMapOf<Int, UserClub>()
        executeQuery(statement, arrayOf(season)) {
            result[it.getInt("uid")] = UserClub(
                it.getInt("club_id"),
                it.getString("name"),
                it.getDouble("point_total"),
                it.getDouble("point_current_season")
            )
        }
        return result
    }

    override fun joinClub(uid: Int, clubId: Int, season: Int) {
        val statement = """
            INSERT INTO user_club_members_v2 (uid, club_id, season)
            VALUES (?, ?, ?)
            ON CONFLICT (uid, club_id, season)
            DO UPDATE SET point = 0, is_leave = 0
            """
        executeUpdate(statement, arrayOf(uid, clubId, season))
    }

    override fun leaveClub(uid: Int) {
        val statement = """
            UPDATE user_club_members_v2
            SET is_leave = 1
            WHERE uid = ?;
            """
        executeUpdate(statement, arrayOf(uid))
    }

    override fun addClubPoint(clubId: Int, season: Int, point: Double) {
        val statement = """
            INSERT INTO user_club_point_v2 (club_id, season, point)
            VALUES (?, ?, ?)
            ON CONFLICT (club_id, season) 
            DO UPDATE SET point = point + EXCLUDED.point;
            """
        executeUpdate(statement, arrayOf(clubId, season, point))
    }

    override fun addMemberClubPoint(uid: Int, clubId: Int, season: Int, point: Double) {
        val statement = """
            INSERT INTO user_club_members_v2 (uid, club_id, season, point)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (uid, club_id, season) 
                DO UPDATE SET point = user_club_members_v2.point + EXCLUDED.point;
            """
        executeUpdate(statement, arrayOf(uid, clubId, season, point))
    }

    override fun summaryClubPoint(season: Int) {
        val statement = """
            WITH club_point AS (SELECT club_id, season, SUM(point) AS total_point FROM user_club_members_v2 WHERE season = ? GROUP BY club_id, season)
            INSERT INTO user_club_point_v2 (club_id, season, point)
            SELECT club_id, season, total_point FROM club_point
            ON CONFLICT (club_id, season)
                DO UPDATE SET point = user_club_point_v2.point + EXCLUDED.point;
            """
        executeUpdate(statement, arrayOf(season))
    }

    override fun getBidPrice(): Map<Int, Int> {
        val statement = """
            SELECT *
            FROM "config_bid_club_package"
            ORDER BY bid_quantity DESC
            """
        val result = mutableMapOf<Int, Int>()
        executeQuery(statement, arrayOf()) {
            result[it.getInt("package_id")] = it.getInt("bid_quantity")
        }
        return result
    }

    override fun getTopClubBid(): MutableMap<Int, Int> {
        val statement = """
            SELECT *
            FROM "user_club_bid_date_v2"
            WHERE date = CURRENT_DATE - 1
            ORDER BY bid_point DESC
            LIMIT 10
            """
        val result = mutableMapOf<Int, Int>()
        executeQuery(statement, arrayOf()) {
            result[it.getInt("club_id")] = it.getInt("bid_point")
        }
        return result
    }

    override fun getCurrentClubBid(): MutableMap<Int, Int> {
        val statement = """
            SELECT *
            FROM "user_club_bid_date_v2"
            WHERE date = CURRENT_DATE
            """
        val result = mutableMapOf<Int, Int>()
        executeQuery(statement, arrayOf()) {
            result[it.getInt("club_id")] = it.getInt("bid_point")
        }
        return result
    }

    override fun addClubBidPoint(clubId: Int, bidPoint: Int) {
        val statement = """
            INSERT INTO user_club_bid_date_v2 (club_id, bid_point, date)
            VALUES (?, ?, CURRENT_DATE)
            ON CONFLICT (club_id, date) 
            DO UPDATE SET bid_point = user_club_bid_date_v2.bid_point + excluded.bid_point
            """
        executeUpdate(statement, arrayOf(clubId, bidPoint))
    }
}
