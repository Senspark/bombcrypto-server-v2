package com.senspark.game.db.helper

import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.EnumConstants.*
import com.senspark.game.declare.GameConstants
import com.senspark.game.declare.customEnum.SubscriptionProduct
import com.senspark.game.declare.customEnum.SubscriptionState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.Language
import java.sql.Timestamp

class QueryHelper {

    companion object {

        fun queryAddUpsertUserBlockReward(
            uid: Int,
            rewardType: BLOCK_REWARD_TYPE,
            dataType: DataType,
            quantity: Float,
            reason: String
        ): Pair<String, Array<Any?>> {
            val statement = """
                SELECT fn_add_user_reward(?, ?, ?, ?, ?);
            """.trimIndent()
            return Pair(statement, arrayOf(uid, dataType.name, quantity, rewardType.name, reason))
        }

        fun querySubUserBlockReward(
            uid: Int,
            rewardType: BLOCK_REWARD_TYPE,
            quantity: Float,
            dataType: DataType,
            reason: String
        ): Pair<String, Array<Any?>> {
            val statement = """
                SELECT fn_sub_user_reward(?, ?, ?, ?, ?);
            """.trimIndent()
            val params = arrayOf<Any?>(uid, dataType.name, quantity, rewardType.name, reason)
            return Pair(statement, params)
        }

        fun queryInsertNewHero(hero: Hero, quantity: Int, dataType: DataType): Pair<String, Array<Any?>> {
            val timeRest = Timestamp(hero.timeRest)
            @Language("SQL")
            val statement = """
                SELECT * from fn_insert_new_bomberman(
                ?, -- user id
                ?, -- hero id
                ?, -- data type
                ?, -- details
                ?, -- hero type
                ?, -- level
                ?, -- bomb power
                ?, -- bomb range
                ?, -- stamina
                ?, -- speed
                ?, -- bomb count
                ?, -- ability list
                ?, -- skin
                ?, -- color
                ?, -- rarity
                ?, -- bomb skin
                ?, -- energy
                ?, -- stage
                ?, -- time rest
                ?, -- is active
                ?, -- shield
                ?, -- ability s
                ?, -- shield level
                ? -- quantity
                )
            """.trimIndent()
            val params = arrayOf<Any?>(
                hero.userId,
                hero.heroId,
                dataType.name,
                hero.details.details,
                hero.type.value,
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
                hero.energy,
                hero.stage,
                timeRest,
                hero.isActive,
                hero.shield.toString(),
                hero.abilityHeroSList.toString(),
                hero.shield.level,
                quantity,
            )
            return Pair(statement, params)
        }

        fun queryInsertNewHeroTR(hero: Hero, quantity: Int, dataType: DataType): Pair<String, Array<Any?>> {
            val timeRest = Timestamp(hero.timeRest)
            @Language("SQL")
            val statement = """
                SELECT * from fn_insert_new_hero_tr(
                ?, -- user id
                ?, -- data type
                ?, -- details
                ?, -- hero type
                ?, -- level
                ?, -- bomb power
                ?, -- bomb range
                ?, -- stamina
                ?, -- speed
                ?, -- bomb count
                ?, -- ability list
                ?, -- skin
                ?, -- color
                ?, -- rarity
                ?, -- bomb skin
                ?, -- energy
                ?, -- stage
                ?, -- time rest
                ?, -- is active
                ?, -- shield
                ?, -- ability s
                ?, -- shield level
                ? -- quantity
                )
            """.trimIndent()
            val params = arrayOf<Any?>(
                hero.userId,
                dataType.name,
                hero.details.details,
                hero.type.value,
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
                hero.energy,
                hero.stage,
                timeRest,
                hero.isActive,
                hero.shield.toString(),
                hero.abilityHeroSList.toString(),
                hero.shield.level,
                quantity,
            )
            return Pair(statement, params)
        }

        fun querySubUserGem(uid: Int, amount: Float, reason: String): Pair<String, Array<Any?>> {
            val statement = """SELECT fn_sub_user_gem(?, ?, ?, ?)::text AS sub_gem_json"""
            val params = arrayOf<Any?>(uid, UserType.TR.name, amount, reason)
            return Pair(statement, params)
        }

        fun querySubUserMaterial(uid: Int, quantity: Int, itemId: Int): Pair<String, Array<Any?>> {
            val statement = """
                UPDATE user_material
                SET quantity = quantity - ?
                WHERE uid = ?
                  AND item_id = ?;
            """.trimIndent()
            val params = arrayOf<Any?>(quantity, uid, itemId)
            return Pair(statement, params)
        }

        fun queryUpsertUserSubscription(
            uid: Int,
            product: SubscriptionProduct,
            startTime: Long,
            endTime: Long,
            packageToken: String,
            packageState: SubscriptionState,
        ): Pair<String, Array<Any?>> {
            val statement = """
                INSERT INTO user_subscription(uid, product_id, start_time, end_time, token, state, last_modify)
                VALUES (?,
                        ?,
                        TO_TIMESTAMP(?) AT TIME ZONE 'UTC',
                        TO_TIMESTAMP(?) AT TIME ZONE 'UTC',
                        ?,
                        ?,
                        NOW() AT TIME ZONE 'utc')
                ON CONFLICT (uid,product_id)
                    DO UPDATE SET start_time  = excluded.start_time,
                                  end_time    = excluded.end_time,
                                  token       = excluded.token,
                                  last_modify = excluded.last_modify,
                                  state       = excluded.state;
            """.trimIndent()
            return Pair(statement, arrayOf(uid, product.name, startTime, endTime, packageToken, packageState.name))
        }

        fun queryUpdateLastClaimSubscription(uid: Int): Pair<String, Array<Any?>> {
            val statement = """
                UPDATE user_config
                SET last_claim_subscription = NOW() AT TIME ZONE 'utc'
                WHERE uid = ?;
            """.trimIndent()
            return Pair(statement, arrayOf(uid))
        }

        fun queryClaimEmailAttach(uid: Int, id: String): Pair<String, Array<Any?>> {
            val statement = """
                UPDATE user_mail
                SET is_claim = TRUE
                WHERE id = ?
                  AND uid = ?;
            """.trimIndent()
            return Pair(statement, arrayOf(id, uid))
        }

        fun queryClaimEmailsAttach(uid: Int, ids: List<String>): Pair<String, Array<Any?>> {
            val statement = """
                UPDATE user_mail
                SET is_claim = TRUE
                WHERE id IN (${ids.joinToString(",")})
                  AND uid = ?;
            """.trimIndent()
            return Pair(statement, arrayOf(uid))
        }

        fun queryInsertLogBuyChestGacha(uid: Int, chestName: String, price: Int): Pair<String, Array<Any?>> {
            val stm = """
                INSERT INTO log_buy_chest_gacha(id_user, chest_name, value, buy_time)
                VALUES (?, ?, ?, NOW() AT TIME ZONE 'utc')
            """.trimIndent()
            return Pair(stm, arrayOf(uid, chestName, price))
        }

        fun queryDeleteUserGachaChest(id: Int): Pair<String, Array<Any?>> {
            val stm = """
                UPDATE user_gacha_chest
                SET deleted = 1
                WHERE chest_id = ?;
            """.trimIndent()
            return Pair(stm, arrayOf(id))
        }

        fun queryInsertUserBuyGemTransaction(
            uid: Int,
            billToken: String,
            totalGemsReceive: Int,
            productId: String,
            isSpecialOffer: Boolean,
            isTest: Boolean,
            region: String,
        ): Pair<String, Array<Any?>> {
            val statement = """
                INSERT INTO user_buy_gem_transaction(uid,
                                                     bill_token,
                                                     product_id,
                                                     gems_receive,
                                                     is_special_offer,
                                                     is_test,
                                                     region)
                VALUES (?, ?, ?, ?, ?, ?, ?);
            """.trimIndent()
            return Pair(statement, arrayOf(uid, billToken, productId, totalGemsReceive, isSpecialOffer, isTest, region))
        }

        fun queryDeleteHeroTRAndLogGrind(uid: Int, itemId: Int, heroIds: List<Int>): Pair<String, Array<Any?>> {
            val statement = """
                WITH _delete_bombers AS (
                    DELETE FROM user_bomber
                        WHERE uid = ?
                            AND type = ?
                            AND bomber_id IN (${heroIds.joinToString(",")})
                        RETURNING *),
                     _delete_item_status AS (
                         DELETE
                             FROM user_item_status
                                 WHERE uid = ?
                                     AND id IN (SELECT bomber_id FROM _delete_bombers)
                                     AND item_id = ?)
                INSERT
                INTO log_user_grind_hero (uid, date, hero_count, hero_ids)
                VALUES ((SELECT uid FROM _delete_bombers LIMIT 1),
                        NOW() AT TIME ZONE 'utc',
                        ?,
                        ?);
        """.trimIndent()
            val params = arrayOf<Any?>(
                uid,
                HeroType.TR.value,
                uid,
                itemId,
                heroIds.size,
                Json.encodeToString(heroIds)
            )
            return Pair(statement, params)
        }
    }
}