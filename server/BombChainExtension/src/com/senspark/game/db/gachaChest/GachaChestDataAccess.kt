package com.senspark.game.db.gachaChest

import com.senspark.common.IDatabase
import com.senspark.common.utils.ILogger
import com.senspark.game.data.model.user.UserGachaChest
import com.senspark.game.declare.customEnum.GachaChestType
import com.senspark.game.schema.TableUserGachaChest
import com.senspark.game.user.IGachaChestManager
import com.senspark.lib.db.BaseDataAccess
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant

class GachaChestDataAccess(
    database: IDatabase,
    log: Boolean,
    logger: ILogger,
) : BaseDataAccess(database, log, logger), IGachaChestDataAccess {

    override fun initialize() {
    }

    override fun addGachaChestForUser(
        uid: Int,
        chestType: GachaChestType,
        currentUserChestSlot: Int,
        gachaChestManager: IGachaChestManager,
        isCanOpenNow: Boolean
    ): UserGachaChest? {
        val statement = """
            WITH _current_chest AS (SELECT COUNT(*) AS number_chest
                                    FROM user_gacha_chest
                                    WHERE uid = ? and deleted = 0)
            INSERT
            INTO user_gacha_chest(uid, chest_type, open_time)
            SELECT ?, ?, ?
            WHERE (SELECT number_chest FROM _current_chest) < ?
            RETURNING *;
        """.trimIndent()
        val params = arrayOf<Any?>(uid, uid, chestType.value, if (isCanOpenNow) 0 else -1, currentUserChestSlot)
        val listUserGachaChest = mutableListOf<UserGachaChest>()
        executeQueryAndThrowException(statement, params) {
            listUserGachaChest.add(UserGachaChest.fromResulSet(it, gachaChestManager))
        }
        if (listUserGachaChest.isNotEmpty()) {
            return listUserGachaChest[0]
        }
        return null
    }


    override fun getUserGachaChests(uid: Int, gachaChestManager: IGachaChestManager): List<UserGachaChest> {
        val result = mutableListOf<UserGachaChest>()
        transaction {
            val select = TableUserGachaChest.selectAll().where {
                (TableUserGachaChest.userId eq uid)
                    .and(TableUserGachaChest.isDeleted eq 0)
            }.orderBy(TableUserGachaChest.chestId)
            select.forEach {
                val type = GachaChestType.fromValue(it[TableUserGachaChest.chestType])
                result.add(
                    UserGachaChest(
                        it[TableUserGachaChest.chestId],
                        gachaChestManager.getChest(type),
                        type,
                        it[TableUserGachaChest.openTime]
                    )
                )
            }
        }
        return result
    }

    override fun startOpeningGachaChest(userGachaChest: UserGachaChest) {
        val currentTime = Instant.now().toEpochMilli()
        transaction {
            TableUserGachaChest.update({ TableUserGachaChest.chestId eq userGachaChest.id }) {
                it[openTime] = currentTime
            }
        }
        userGachaChest.openTime = currentTime
    }

    override fun skipOpenTimeByGem(
        uid: Int,
        chestId: Int,
        gemPrice: Int
    ) {
        transaction {
            executeQueryAndThrowException(
                "SELECT * from fn_sub_user_gem($uid, 'TR', $gemPrice, 'Skip open time')",
                emptyArray()
            ) {}
            TableUserGachaChest.update({ TableUserGachaChest.chestId eq chestId }) {
                it[openTime] = 0
            }
        }
    }

    override fun skipOpenTimeByAds(
        uid: Int,
        chest: UserGachaChest,
        skipTimeInMilli: Long
    ): UserGachaChest {
        transaction {
            chest.openTime = chest.openTime - skipTimeInMilli
            if (chest.remainingOpenTime <= 0)
                chest.openTime = 0
            TableUserGachaChest.update({ TableUserGachaChest.chestId eq chest.id }) {
                it[openTime] = chest.openTime
            }
        }
        return chest
    }
}