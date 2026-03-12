package com.senspark.game.pvp.manager

import com.senspark.common.IDatabase
import com.senspark.common.pvp.IMatchStats
import com.senspark.common.utils.ILogger
import com.senspark.game.api.IPvpResultInfo
import com.senspark.game.api.IPvpResultUserInfo
import com.senspark.game.pvp.entity.HeroDamageSource
import com.senspark.game.schema.TableLogPlayPvp
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class DefaultDatabaseManager(
    private val _db: IDatabase,
    private val _log: Boolean,
    private val _logger: ILogger
) : IDatabaseManager {
    private fun parseUsername(info: IPvpResultUserInfo): String {
        return if (info.isBot) "-1" else info.username
    }

    private fun parseLoseReason(info: IPvpResultInfo, slot: Int): String {
        if (info.winningTeam == slot) {
            return ""
        }
        val userInfo = info.info[slot]
        if (userInfo.quit) {
            return "quit"
        }
        return when (HeroDamageSource.values()[userInfo.damageSource]) {
            HeroDamageSource.Bomb -> "bomb"
            HeroDamageSource.HardBlock -> "block_drop"
            HeroDamageSource.PrisonBreak -> "hero"
            else -> {
                require(false) { "Failed to infer lose reason" }
                "unknown"
            }
        }
    }

    override fun addMatch(info: IPvpResultInfo, stats: IMatchStats) {
        transaction {
            TableLogPlayPvp.insert {
                it[this.matchId] = info.id
                it[this.serverId] = info.serverId
                it[this.matchResult] = if (info.isDraw) "draw" else "complete"
                it[this.matchTimestamp] = Instant.ofEpochMilli(info.timestamp)
                it[this.matchDuration] = info.duration
                it[this.username1] = parseUsername(info.info[0])
                it[this.username2] = parseUsername(info.info[1])
                it[this.heroId1] = info.info[0].heroId
                it[this.heroId2] = info.info[1].heroId
                it[this.collectedItem1] = info.info[0].collectedItems.size
                it[this.collectedItem2] = info.info[1].collectedItems.size
                it[this.loseReason1] = parseLoseReason(info, 0)
                it[this.loseReason2] = parseLoseReason(info, 1)
                it[this.deltaPoint1] = info.info[0].deltaPoint
                it[this.deltaPoint2] = info.info[1].deltaPoint
                it[this.latency] = stats.userStats.map { info -> info.latency }.toTypedArray()
                it[this.timeDelta] = stats.userStats.map { info -> info.timeDelta }.toTypedArray()
                it[this.lossRate] = stats.userStats.map { info -> info.lossRate }.toTypedArray()
                it[this.countryCode] = stats.userStats.map { info -> info.countryIsoCode }.toTypedArray()
            }
        }

        // Legacy.
        // Update last played hero.
        val queryBuilder = _db.createQueryBuilder(_log)
        val statementUserPvp = """
            INSERT INTO user_pvp(uid, last_played_hero_id)
            VALUES ((SELECT id_user FROM "user" WHERE user_name = ?), ?)
            ON CONFLICT (uid) DO UPDATE SET last_played_hero_id = excluded.last_played_hero_id;
        """.trimIndent()
        info.info
            .filter { !it.isBot }
            .forEach {
                queryBuilder.addStatementUpdate(statementUserPvp, arrayOf(it.username, it.heroId))
            }
        queryBuilder.executeMultiQuery()
    }
}