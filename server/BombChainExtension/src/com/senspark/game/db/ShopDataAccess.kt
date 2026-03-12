package com.senspark.game.db

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.senspark.common.IDatabase
import com.senspark.common.constant.PvPItemType
import com.senspark.common.pvp.IPvpFixtureMatchInfo
import com.senspark.common.pvp.IRankManager
import com.senspark.common.pvp.PvpFixtureMatchInfo
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.ItemKind
import com.senspark.game.constant.ItemType
import com.senspark.game.controller.IUserController
import com.senspark.game.data.HeroUpgradeShieldData
import com.senspark.game.data.manager.iap.FreeRewardConfigItem
import com.senspark.game.data.manager.iap.IAPGoldShopConfigItem
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.model.config.*
import com.senspark.game.data.model.config.AutoMinePackage
import com.senspark.game.data.model.nft.ConfigHeroTraditional
import com.senspark.game.data.model.user.IUserInfo
import com.senspark.game.db.helper.QueryHelper
import com.senspark.game.db.model.GachaChestSlotType
import com.senspark.game.declare.EnumConstants.*
import com.senspark.game.declare.customEnum.*
import com.senspark.game.declare.customTypeAlias.ProductId
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.senspark.game.manager.rock.RockAmount
import com.senspark.game.schema.*
import com.senspark.game.utils.deserializeList
import com.senspark.game.utils.deserializeSet
import com.senspark.lib.db.BaseDataAccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Timestamp
import java.time.Instant
import java.time.ZoneOffset

class ShopDataAccess(
    database: IDatabase,
    val log: Boolean,
    logger: ILogger,
) : BaseDataAccess(database, log, logger), IShopDataAccess {

    override fun initialize() {
    }

    override fun loadBlock(): HashMap<DataType, HashMap<Int, BlockConfig>> {
        val result = HashMap<DataType, HashMap<Int, BlockConfig>>()
        val statement = "SELECT * from config_block;"
        executeQuery(statement, arrayOf()) {
            try {
                val dataType = DataType.valueOf(it.getString("type"))
                val items = result[dataType] ?: HashMap()
                val block = BlockConfig.fromResulSet(it)
                items[block.id] = block
                result[dataType] = items
            } catch (e: Exception) {
                // ignore
            }

        }
        return result
    }

    override fun loadResetShieldBomber(): Map<Int, ResetShieldBomber> {
        val result: MutableMap<Int, ResetShieldBomber> = HashMap()
        val statement = "SELECT * FROM config_reset_shield_balancer order by rare;"
        executeQuery(statement, arrayOf()) {
            val resetShieldBomber = ResetShieldBomber.fromResulSet(it)
            result[resetShieldBomber.getRare()] = resetShieldBomber
        }
        return result
    }

    override fun loadStakeVipRewards(): Map<Int, MutableList<StakeVipReward>> {
        val result: MutableMap<Int, MutableList<StakeVipReward>> = HashMap()
        val statement = "SELECT * FROM config_stake_vip_reward"
        executeQuery(statement, arrayOf()) {
            val level = it.getInt("level")
            var items: MutableList<StakeVipReward> = ArrayList()
            if (result.containsKey(level)) {
                items = result[level]!!
            }
            items.add(
                StakeVipReward.fromResulSet(it)
            )
            result[level] = items
        }
        return result
    }

    override fun loadHeroUpgradePower(): Map<Int, HeroUpgradePower> {
        val result: MutableMap<Int, HeroUpgradePower> = HashMap()
        val statement = "SELECT * FROM config_hero_upgrade_power order by rare;"
        executeQuery(statement, arrayOf()) {
            val upgradePower = HeroUpgradePower()
            upgradePower.rare = it.getInt("rare")
            val datas = it.getString("datas")
            val costLst = deserializeList<Int>(datas)
            upgradePower.powers = costLst
            result[upgradePower.rare] = upgradePower
        }
        return result
    }

    override fun loadBomberAbility(): Map<Int, HeroAbilityConfig> {
        val result: MutableMap<Int, HeroAbilityConfig> = HashMap()
        val statement = "SELECT * FROM config_bomber_ability"
        executeQuery(statement, arrayOf()) {
            val ability = HeroAbilityConfig(it.getInt("ability"), it.getDouble("values").toFloat())
            result[ability.id] = ability
        }
        return result
    }

    override fun loadGachaChestSlots(): Map<Int, GachaChestSlot> {
        val result = HashMap<Int, GachaChestSlot>()
        val statement = """SELECT * FROM config_gacha_chest_slot;"""
        executeQueryAndThrowException(statement, emptyArray()) {
            val slot = it.getInt("slot")
            result[slot] = GachaChestSlot(
                slot = slot,
                type = GachaChestSlotType.valueOf(it.getString("type")),
                price = it.getInt("price")
            )
        }
        return result
    }

    override fun loadAdventureEnemyCreator(): Map<Int, EnemyCreator> {
        val result: MutableMap<Int, EnemyCreator> = HashMap()
        val statement = "SELECT * FROM config_adventure_mode_entity_creator"
        executeQuery(statement, arrayOf()) {
            result[it.getInt("entity_id")] = EnemyCreator.fromResulSet(it)
        }
        return result
    }

    override fun loadReviveHeroCosts(): Map<Int, ReviveHeroCost> {
        val result = mutableMapOf<Int, ReviveHeroCost>()
        val statement = """SELECT * FROM config_revive_hero_cost;"""
        executeQueryAndThrowException(statement, emptyArray()) {
            val times = it.getInt("times")
            result[times] = ReviveHeroCost(
                times = times,
                allowAds = it.getInt("allow_ads") == 1,
                gemAmount = it.getInt("gem_amount")
            )
        }
        return result
    }

    override fun loadDailyMission(): Map<String, Mission> {
        val results: MutableMap<String, Mission> = HashMap()
        val statement = """
            SELECT cdm.code,
                   cdm.action,
                   cdm.type,
                   cdm.number_mission,
                   cdm.rewards::text,
                   cdm.sort,
                   cdm.active,
                   cdm.cool_down,
                   cdm.next_mission_code,
                   cdm.previous_mission_code,
                   ROW_TO_JSON(cm)::text AS description
            FROM config_daily_mission AS cdm
                     INNER JOIN config_message cm ON cdm.message_code = cm.code
            WHERE cdm.active;
        """.trimIndent()
        database.createQueryBuilder()
            .addStatement(statement, emptyArray())
            .executeQuery {
                val item = Mission.fromResultSet(it)
                results[item.code] = item
            }
        return results
    }

    override fun loadAdventureLevelStrategy(): Map<Int, Map<Int, LevelStrategy>> {
        val result: MutableMap<Int, MutableMap<Int, LevelStrategy>> = HashMap()
        val statement = "SELECT * FROM config_adventure_mode_level_strategy"
        database.createQueryBuilder()
            .addStatement(statement, emptyArray())
            .executeQuery {
                val config = LevelStrategy.fromResultSet(it)
                result.getOrPut(config.stage) { mutableMapOf() }.apply {
                    put(config.level, config)
                }
            }
        return result
    }

    override fun loadBlockDropByDay(): HashMap<DataType, List<BlockDropRate>> {
        val result = HashMap<DataType, List<BlockDropRate>>()
        val statement = "SELECT * from config_block_drop_by_day;"
        val params = arrayOf<Any?>()
        executeQuery(statement, params) {
            try {
                val dataType = DataType.valueOf(it.getString("type"))
                val items = result[dataType]?.toMutableList() ?: ArrayList()
                val day = it.getInt("days")
                val dropRate = deserializeList<Int>(it.getString("datas"))
                val block = BlockDropRate(day, dropRate)
                items.add(block)
                result[dataType] = items
            } catch (e: Exception) {
                // ignore
            }
        }
        return result
    }

    override fun loadBlockReward(): HashMap<DataType, HashMap<Int, MutableList<IBlockReward>>> {
        val result = HashMap<DataType, HashMap<Int, MutableList<IBlockReward>>>()
        val statement = "SELECT * from config_block_reward;"
        val params = arrayOf<Any?>()
        executeQueryAndThrowException(statement, params) {
            try {
                // group by data_type
                val dataType = DataType.valueOf(it.getString("data_type"))
                var rewardByDataType = result[dataType]
                if (rewardByDataType == null) {
                    rewardByDataType = HashMap()
                    result[dataType] = rewardByDataType
                }

                // group by block_id
                val blockId = it.getInt("block_id")
                var rewardByBlockId = rewardByDataType[blockId]
                if (rewardByBlockId == null) {
                    rewardByBlockId = arrayListOf()
                    rewardByDataType[blockId] = rewardByBlockId
                }

                rewardByBlockId.add(BlockReward.fromResultSet(it))
                rewardByBlockId.sortBy { block -> block.weight }
            } catch (e: Exception) {
                // ignore
            }
        }
        return result
    }

    override fun loadRankingSeason(): MutableMap<Int, Season> {
        val result: MutableMap<Int, Season> = HashMap()
        val statement = """
            SELECT ps.id,
                   (EXTRACT(EPOCH FROM ps.start_date) * 1000) ::BIGINT AS start_date,
                   (EXTRACT(EPOCH FROM ps.end_date) * 1000)::BIGINT    AS end_date,
                   (EXTRACT(EPOCH FROM ns.start_date) * 1000)::BIGINT  AS next_season_start_date
            FROM config_ranking_season AS ps
                     LEFT JOIN config_ranking_season AS ns ON ps.id = ns.id - 1;
        """.trimIndent()
        executeQuery(statement, arrayOf()) {
            val season = Season.fromResultSet(it)
            result[season.id] = season
        }
        return result
    }

    override fun addNewPVPRankingSeason(newSeasonId: Int, timeStartNextSeason: Instant, timeEndNextSeason: Instant) {
        val statement = """
            INSERT INTO config_ranking_season (id, start_date, end_date)
            VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING;
            """
        executeUpdate(
            statement,
            arrayOf(newSeasonId, Timestamp.from(timeStartNextSeason), Timestamp.from(timeEndNextSeason))
        )
    }

    override fun loadCoinRankingSeason(): MutableMap<Int, Season> {
        val result: MutableMap<Int, Season> = HashMap()
        val statement = """
            SELECT ps.id,
                   (EXTRACT(EPOCH FROM ps.start_date) * 1000) ::BIGINT AS start_date,
                   (EXTRACT(EPOCH FROM ps.end_date) * 1000)::BIGINT    AS end_date,
                   (EXTRACT(EPOCH FROM ns.start_date) * 1000)::BIGINT  AS next_season_start_date
            FROM config_coin_ranking_season AS ps
                     LEFT JOIN config_coin_ranking_season AS ns ON ps.id = ns.id - 1;
        """.trimIndent()
        executeQuery(statement, arrayOf()) {
            val season = Season.fromResultSet(it)
            result[season.id] = season
        }
        return result
    }

    override fun queryHeroUpgradeShield(): List<HeroUpgradeShieldData> {
        val statement = "SELECT * FROM config_hero_upgrade_shield order by rarity;"
        val results: MutableList<HeroUpgradeShieldData> = ArrayList()
        val typeValues = object : TypeToken<List<Int?>?>() {}.type
        val typePrices = object : TypeToken<List<Float?>?>() {}.type
        executeQuery(statement, arrayOf()) {
            results.add(
                HeroUpgradeShieldData(
                    it.getInt("rarity"),
                    Gson().fromJson(it.getString("data"), typeValues),
                    Gson().fromJson(it.getString("price"), typePrices)
                )
            )
        }
        return results
    }

    override fun loadHeroRepairShield(): Map<Int, Map<Int, HeroRepairShield>> {
        val result: MutableMap<Int, Map<Int, HeroRepairShield>> = HashMap()
        val statement = "SELECT *, price_rock FROM config_hero_repair_shield"
        executeQuery(statement, arrayOf()) {
            val heroRepairShield = HeroRepairShield.fromResultSet(it)
            val items: MutableMap<Int, HeroRepairShield> =
                (if (result[heroRepairShield.rarity] == null) HashMap() else result[heroRepairShield.rarity])!!.toMutableMap()
            items[heroRepairShield.shieldLevel] = heroRepairShield
            result[heroRepairShield.rarity] = items
        }
        return result
    }

    override fun getHeroTraditionalConfigs(): Map<Int, ConfigHeroTraditional> {
        return transaction {
            TableHeroTraditional
                .selectAll()
                .associate {
                    it[TableHeroTraditional.itemId] to ConfigHeroTraditional(
                        itemId = it[TableHeroTraditional.itemId],
                        skin = it[TableHeroTraditional.skin],
                        color = it[TableHeroTraditional.color],
                        speed = it[TableHeroTraditional.speed],
                        range = it[TableHeroTraditional.range],
                        bomb = it[TableHeroTraditional.bomb],
                        hp = it[TableHeroTraditional.hp],
                        dmg = it[TableHeroTraditional.dmg],
                        maxSpeed = it[TableHeroTraditional.maxSpeed],
                        maxRange = it[TableHeroTraditional.maxRange],
                        maxBomb = it[TableHeroTraditional.maxBomb],
                        maxHp = it[TableHeroTraditional.maxHp],
                        maxDmg = it[TableHeroTraditional.maxDmg],
                        maxUpgradeSpeed = it[TableHeroTraditional.maxUpgradeSpeed],
                        maxUpgradeRange = it[TableHeroTraditional.maxUpgradeRange],
                        maxUpgradeBomb = it[TableHeroTraditional.maxUpgradeBomb],
                        maxUpgradeHp = it[TableHeroTraditional.maxUpgradeHp],
                        maxUpgradeDmg = it[TableHeroTraditional.maxUpgradeDmg],
                        tutorial = it[TableHeroTraditional.tutorial],
                        canBeBot = it[TableHeroTraditional.canBeBot] == 1,
                    )
                }
        }
    }

    override fun getIAPGemShopConfigs(): Map<IAPShopType, Map<ProductId, IAPShopConfig>> {
        val result = mutableMapOf<IAPShopType, MutableMap<ProductId, IAPShopConfig>>()
        val select = """
            SELECT product_id,
                   type,
                   name,
                   items::text,
                   items_bonus::text,
                   limit_per_user,
                   bonus_type,
                   is_stater_pack,
                   is_remove_ads,
                   buy_step,
                   purchase_time_limit
            FROM config_iap_shop;
        """.trimIndent()
        database.createQueryBuilder(log)
            .addStatement(select, emptyArray())
            .executeQuery {
                val config = IAPShopConfig.fromResultSet(it)
                result.getOrPut(config.type) { mutableMapOf() }.apply {
                    put(config.productId, config)
                }
            }
        return result
    }

    override fun getIAPGoldShopConfigs(): List<IAPGoldShopConfigItem> {
        val result = mutableListOf<IAPGoldShopConfigItem>()
        val select = "SELECT * FROM config_iap_gold_shop"
        executeQuery(select, emptyArray()) {
            result.add(IAPGoldShopConfigItem.fromResultSet(it))
        }
        return result
    }

    override fun getFreeRewardConfigs(): List<FreeRewardConfigItem> {
        val result = mutableListOf<FreeRewardConfigItem>()
        val select = "SELECT * FROM config_free_reward_by_ads"
        executeQuery(select, emptyArray()) {
            result.add(FreeRewardConfigItem.fromResultSet(it))
        }
        return result
    }

    override fun buyGoldByGem(
        userInfo: IUserInfo,
        userBlockRewardManager: IUserBlockRewardManager,
        configItem: IAPGoldShopConfigItem
    ) {
        transaction {
            val gem = userBlockRewardManager.get(BLOCK_REWARD_TYPE.GEM)?.values ?: 0F
            val gemLocked = userBlockRewardManager.get(BLOCK_REWARD_TYPE.GEM_LOCKED)?.values ?: 0F
            if (configItem.gemPrice > (gem + gemLocked))
                throw CustomException("Not enough ${configItem.gemPrice} Gem")
            executeQueryAndThrowException(
                "SELECT * from fn_sub_user_gem(${userInfo.id}, 'TR', ${configItem.gemPrice}, '${ChangeRewardReason.BUY_GOLD}')",
                emptyArray()
            ) {}
            upsertUserBlockReward(
                userInfo.id,
                configItem.goldsReceive,
                BLOCK_REWARD_TYPE.GOLD,
                ChangeRewardReason.BUY_GOLD
            )
            userBlockRewardManager.loadUserBlockReward()
        }
    }

    private fun upsertUserBlockReward(userId: Int, value: Int, type: BLOCK_REWARD_TYPE, reason: String) {
        val query = QueryHelper.queryAddUpsertUserBlockReward(
            userId,
            type,
            DataType.TR,
            value.toFloat(),
            reason
        )
        executeQueryAndThrowException(query.first, query.second) {}
    }

    override fun loadAdventureItem(): Map<PvPItemType, AdventureItem> {
        val statement = """SELECT * FROM config_adventure_mode_items;"""
        val result: MutableMap<PvPItemType, AdventureItem> = mutableMapOf()
        executeQueryAndThrowException(statement, emptyArray()) {
            val type = PvPItemType.valueOf(it.getString("type"))
            result[type] = AdventureItem.fromResultSet(it)
        }
        return result.toMap()
    }

    override fun getConfigItem(): Map<Int, Item> {
        return transaction {
            TableConfigItem.selectAll().where(
                TableConfigItem.active eq true
            )
                .filter { ItemType.hasValue(it[TableConfigItem.type]) }
                .associate {
                    it[TableConfigItem.id] to Item(
                        it[TableConfigItem.id],
                        ItemType.fromValue(it[TableConfigItem.type]),
                        it[TableConfigItem.name],
                        deserializeSet(it[TableConfigItem.ability]),
                        ItemKind.valueOf(it[TableConfigItem.kind]),
                        it[TableConfigItem.descriptionEn],
                        it[TableConfigItem.goldPrice7Days],
                        it[TableConfigItem.gemPrice7Days],
                        it[TableConfigItem.gemPrice30Days],
                        it[TableConfigItem.gemPrice],
                        it[TableConfigItem.goldPrice],
                        it[TableConfigItem.isSellable],
                        it[TableConfigItem.tag],
                        it[TableConfigItem.isDefault],
                        it[TableConfigItem.saleStartDate]?.atStartOfDay(ZoneOffset.UTC)?.toEpochSecond(),
                        it[TableConfigItem.saleEndDate]?.atStartOfDay(ZoneOffset.UTC)?.toEpochSecond(),
                    )
                }
        }
    }

    override fun loadLuckyWheelReward(configItemManager: IConfigItemManager): List<LuckyWheelReward> {
        return transaction {
            TableConfigLuckyWheelReward.selectAll().where {
                TableConfigLuckyWheelReward.active eq true
            }.sortedBy {
                TableConfigLuckyWheelReward.sort
            }.map { LuckyWheelReward.fromResultRow(it, configItemManager) }
        }
    }

    override fun addFreeReward(
        userController: IUserController,
        freeRewardConfigItem: FreeRewardConfigItem,
        reason: String
    ) {
        transaction {
            val blockRewardManager = userController.masterUserManager.blockRewardManager
            val rewardType = BLOCK_REWARD_TYPE.valueOf(freeRewardConfigItem.rewardType)
            val query = QueryHelper.queryAddUpsertUserBlockReward(
                userController.userId,
                rewardType,
                DataType.TR,
                freeRewardConfigItem.quantityPerView.toFloat(),
                reason
            )
            executeQueryAndThrowException(query.first, query.second) {}
            blockRewardManager.loadUserBlockReward()
            val userConfigManager = userController.masterUserManager.userConfigManager
            userConfigManager.changeUserFreeRewardOpenTimeConfigToNow(rewardType)
            val userFreeRewardConfigJson = Json.encodeToString(userConfigManager.freeRewardConfig)
            val updateStatement = """
                INSERT INTO user_config(uid, free_reward_config) values (?, ?)
                ON CONFLICT (uid)
                DO UPDATE SET free_reward_config = EXCLUDED.free_reward_config
            """.trimIndent()
            executeUpdateThrowException(updateStatement, arrayOf(userController.userId, userFreeRewardConfigJson))
        }
    }

    override fun loadMysteryBox(configItemManager: IConfigItemManager): List<MysteryBox> {
        return transaction {
            TableMysteryBox.selectAll()
                .map { MysteryBox.fromResultRow(it, configItemManager) }
        }
    }

    override fun loadNewUserGift(configItemManager: IConfigItemManager): List<NewUserGift> {
        return transaction {
            TableNewUserGift
                .selectAll().where(TableNewUserGift.active eq true)
                .map { NewUserGift.fromResultRow(it, configItemManager) }
        }
    }

    override fun loadGrindHeroConfig(configItemManager: IConfigItemManager): Map<ItemKind, GrindHero> {
        return transaction {
            TableGrindHero.selectAll()
                .map { GrindHero.fromResultRow(it, configItemManager) }.associateBy { it.itemKind }
        }
    }

    override fun loadUpgradeCrystalConfig(): Map<Int, UpgradeCrystal> {
        val result = mutableMapOf<Int, UpgradeCrystal>()
        val stm = """
            SELECT *
            FROM config_upgrade_crystal;
        """.trimIndent()
        database.createQueryBuilder()
            .addStatement(stm, emptyArray())
            .executeQuery {
                val item = UpgradeCrystal.fromResultSet(it)
                result[item.sourceItemId] = item
            }
        return result
    }

    override fun loadUpgradeHeroTrConfig(): Map<ConfigUpgradeHeroType, Map<Int, UpgradeHeroTr>> {
        val result = mutableMapOf<ConfigUpgradeHeroType, MutableMap<Int, UpgradeHeroTr>>()
        val stm = """
            SELECT *
            FROM config_upgrade_hero_tr;
        """.trimIndent()
        database.createQueryBuilder()
            .addStatement(stm, emptyArray())
            .executeQuery {
                val item = UpgradeHeroTr.fromResultSet(it)
                val items = result[item.type] ?: mutableMapOf()
                items[item.index] = item
                result[item.type] = items
            }
        return result
    }

    override fun loadSubscription(): MutableMap<SubscriptionProduct, SubscriptionPackage> {
        val result = mutableMapOf<SubscriptionProduct, SubscriptionPackage>()
        val statement = """
            SELECT * FROM config_subscription;
        """.trimIndent()
        database.createQueryBuilder()
            .addStatement(statement, emptyArray())
            .executeQuery {
                val item = SubscriptionPackage.fromResultSet(it)
                result[item.id] = item
            }
        return result
    }

    override fun loadPvpFixture(rankManager: IRankManager, currentSeasonNumber: Int): List<IPvpFixtureMatchInfo> {
        val result = mutableListOf<IPvpFixtureMatchInfo>()
        val statement = """
            WITH _cpt AS (
                SELECT
                    id,
                    mode,
                    participant_1,
                    participant_2,
                    status,
                    EXTRACT(EPOCH FROM find_begin_time) * 1000 AS find_begin_timestamp,
                    EXTRACT(EPOCH FROM find_end_time) * 1000 AS find_end_timestamp,
                    EXTRACT(EPOCH FROM finish_time) * 1000 AS finish_timestamp,
                    user_1_score,
                    user_2_score
                FROM config_pvp_tournament
            )
            SELECT
                cpt.*,
                u1.id_user AS user_id_1,
                u2.id_user AS user_id_2,
                u1.user_name AS username_1,
                u2.user_name AS username_2,
                COALESCE(u1.name, u1.second_username, u1.user_name) AS display_name_1,
                COALESCE(u2.name, u2.second_username, u2.user_name) AS display_name_2,
                COALESCE(uprs1.point, 0) AS rank_point_1,
                COALESCE(uprs2.point, 0) AS rank_point_2
            FROM _cpt AS cpt
            INNER JOIN "user" AS u1 ON (cpt.participant_1 = u1.user_name OR cpt.participant_1 = u1.second_username)
            INNER JOIN "user" AS u2 ON (cpt.participant_2 = u2.user_name OR cpt.participant_2 = u2.second_username)
            LEFT JOIN user_pvp_rank_ss_$currentSeasonNumber AS uprs1 ON u1.id_user = uprs1.uid
            LEFT JOIN user_pvp_rank_ss_$currentSeasonNumber AS uprs2 ON u2.id_user = uprs2.uid;
        """.trimIndent()
        database.createQueryBuilder()
            .addStatement(statement, emptyArray())
            .executeQuery {
                result.add(PvpFixtureMatchInfo.fromResultSet(it, rankManager))
            }
        return result
    }

    override fun loadGachaChestConfigs(configItemManager: IConfigItemManager): Map<GachaChestType, IGachaChest> {
        val result: MutableMap<GachaChestType, GachaChest> = mutableMapOf()
        val select = """
            WITH _cgc AS (SELECT *
                          FROM config_gacha_chest),
                 _items AS (SELECT _cgc.type,
                                   JSON_AGG(ROW_TO_JSON(cgci)) AS items
                            FROM _cgc
                                     INNER JOIN config_gacha_chest_items cgci ON _cgc.type = cgci.chest_type
                            GROUP BY _cgc.type)
            SELECT _cgc.*,
                   _items.items::text
            FROM _cgc
                     INNER JOIN
                 _items ON _cgc.type = _items.type;
        """.trimIndent()
        database.createQueryBuilder(log)
            .addStatement(select, emptyArray())
            .executeQuery {
                val chest = GachaChest.fromResultSet(it, configItemManager)
                result[chest.type] = chest
            }
        return result
    }

    override fun loadCostumePresetPrice(): Map<BLOCK_REWARD_TYPE, Int> {
        return mapOf(BLOCK_REWARD_TYPE.GOLD to 350, BLOCK_REWARD_TYPE.GEM to 26)
    }

    override fun loadMinStakeHeroConfig(): Map<Int, Int> {
        val result: MutableMap<Int, Int> = HashMap()
        val statement = "SELECT * FROM config_min_stake_hero order by rarity;"
        executeQuery(statement, arrayOf()) {
            result[it.getInt("rarity")] = it.getInt("min_stake_amount")
        }
        return result
    }

    override fun loadSwapTokenConfig(): List<SwapTokenConfig> {
        val statement = "SELECT * FROM config_swap_token;"
        val result: MutableList<SwapTokenConfig> = ArrayList()
        executeQuery(statement, arrayOf()) {
            result.add(SwapTokenConfig.fromResultSet(it))
        }
        return result
    }

    override fun loadSwapTokenRealtimeConfig(): SwapTokenRealtimeConfig {
        val statement = "SELECT * FROM config_swap_token_realtime;"
        val resultHasMap: HashMap<String, String> = HashMap()
        executeQuery(statement, arrayOf()) {
            val key = it.getString("key")
            val value = it.getString("value")
            resultHasMap[key] = value
        }

        return SwapTokenRealtimeConfig(resultHasMap)
    }

    override fun loadAutoMinePackageConfig(): Map<DataType, List<AutoMinePackage>> {
        val statement = "SELECT * FROM config_package_auto_mine;"
        val result: MutableMap<DataType, ArrayList<AutoMinePackage>> = mutableMapOf()
        executeQuery(statement, arrayOf()) {
            try {
                val type = DataType.valueOf(it.getString("data_type"))
                if (!result.containsKey(type)) {
                    result[type] = arrayListOf()
                }
                result[type]!!.add(
                    AutoMinePackage.fromResultSet(it)
                )
            } catch (e: Exception) {
                // ignore
            }
        }
        return result
    }

    override fun loadBurnHeroConfig(): Map<Int, RockAmount> {
        val statement = "SELECT * FROM config_burn_hero;"
        val result: MutableMap<Int, RockAmount> = mutableMapOf()
        executeQuery(statement, arrayOf()) {
            val rarity = it.getInt("rarity")
            val heroSRock = it.getDouble("hero_s_rock").toFloat()
            val heroLRock = it.getDouble("hero_l_rock").toFloat()
            result[rarity] = RockAmount(heroSRock, heroLRock)
        }
        return result
    }

    override fun loadRockPackageConfig(): List<RockPackage> {
        val statement = "SELECT * FROM config_rock_pack;"
        val result: MutableList<RockPackage> = ArrayList()
        executeQuery(statement, arrayOf()) {
            result.add(RockPackage.fromResultSet(it))
        }
        return result
    }

    override fun loadHouseRentPackageConfig(): Map<DataType, List<HouseRentPackage>> {
        val statement = "SELECT * FROM config_package_rent_house_v2;"
        val result: MutableMap<DataType, ArrayList<HouseRentPackage>> = mutableMapOf()
        executeQuery(statement, arrayOf()) {
            try {
                val type = DataType.valueOf(it.getString("data_type"))
                if (!result.containsKey(type)) {
                    result[type] = arrayListOf()
                }
                result[type]!!.add(
                    HouseRentPackage.fromResultSet(it)
                )
            } catch (e: Exception) {
                // ignore
            }
        }
        return result
    }
}




