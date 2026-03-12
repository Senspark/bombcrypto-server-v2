package com.senspark.game.data.model

import com.senspark.game.data.model.nft.HeroAbilityList
import com.senspark.game.data.model.nft.IHeroDetails
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.EnumConstants.HeroType
import com.senspark.game.utils.WeightedRandom
import com.smartfoxserver.v2.entities.data.ISFSObject
import kotlinx.serialization.json.Json
import kotlin.random.Random

class ServerHeroDetails(
    private var _heroId: Int,
    override val dataType: EnumConstants.DataType,
    override val bombPower: Int,
    override val bombRange: Int,
    override val stamina: Int,
    override val speed: Int,
    override val bombCount: Int,
    override val abilityList: HeroAbilityList,
    override var skin: Int,
    override var color: Int,
    override val rarity: Int,
    override val bombSkin: Int,
    override val type: HeroType
) : IHeroDetails {
    override val heroId
        get() = _heroId
    override val details = ""
    override val hp = 0
    override val dmg = 0
    override val maxHp = 0
    override val maxDmg = 0
    override val maxUpgradeSpeed = 0
    override val maxUpgradeRange = 0
    override val maxUpgradeBomb = 0
    override val maxUpgradeHp = 0
    override val maxUpgradeDmg = 0
    override val upgradedSpeed = 0
    override val upgradedRange = 0
    override val upgradedBomb = 0
    override val upgradedHp = 0
    override val upgradedDmg = 0
    override val maxSpeed = 0
    override val maxRange = 0
    override val maxBomb = 0
    override val resetShieldCounter = 1
    override val shieldLevel = 1
    override val level = 1
    override val index = 3
    override val abilityHeroSList = HeroAbilityList(listOf(1))
    override val heroTRType = EnumConstants.HeroTRType.HERO
    override val heroConfig = null

    companion object {

        class StatsRange(
            val min: Int,
            val max: Int
        )

        class Stats(
            val stamina: StatsRange,
            val speed: StatsRange,
            val bombCount: Int,
            val bombPower: StatsRange,
            val bombRange: Int,
            val ability: Int
        )

        private val rarityStats = mutableMapOf(
            0 to Stats(
                StatsRange(1, 3),
                StatsRange(1, 3),
                1,
                StatsRange(1, 3),
                1,
                1
            ),
            1 to Stats(
                StatsRange(3, 6),
                StatsRange(3, 6),
                2,
                StatsRange(3, 6),
                2,
                2
            ),
            2 to Stats(
                StatsRange(6, 9),
                StatsRange(6, 9),
                3,
                StatsRange(6, 9),
                3,
                3
            ),
            3 to Stats(
                StatsRange(9, 12),
                StatsRange(9, 12),
                4,
                StatsRange(9, 12),
                4,
                4
            ),
            4 to Stats(
                StatsRange(12, 15),
                StatsRange(12, 15),
                5,
                StatsRange(12, 15),
                5,
                5
            ),
            5 to Stats(
                StatsRange(15, 18),
                StatsRange(15, 18),
                6,
                StatsRange(15, 18),
                6,
                6
            ),
            6 to Stats(
                StatsRange(18, 21),
                StatsRange(18, 21),
                7,
                StatsRange(18, 21),
                7,
                7
            ),
            7 to Stats(
                StatsRange(21, 24),
                StatsRange(21, 24),
                8,
                StatsRange(21, 24),
                8,
                7
            ),
            8 to Stats(
                StatsRange(24, 27),
                StatsRange(24, 27),
                9,
                StatsRange(24, 27),
                9,
                7
            ),
            9 to Stats(
                StatsRange(27, 30),
                StatsRange(27, 30),
                10,
                StatsRange(27, 30),
                10,
                7
            ),
        )
        private const val COLOR_COUNT = 5
        private const val BOMB_SKIN_COUNT = 20
        private val abilityIds = mutableListOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        private val abilityRate = mutableListOf(1f, 2f, 1f, 1f, 1f, 2f, 2f, 0f, 0f, 0f)
        private val abilityRateNew = mutableListOf(10f, 15f, 10f, 10f, 10f, 15f, 15f, 5f, 7f, 3f)
        val dropRate = listOf(8287f, 1036f, 518f, 104f, 52f, 4f)
        private val dropRateNew = listOf(8118f, 962f, 481f, 240f, 112f, 51f, 22f, 9f, 4f, 1f)
        private val skinArr = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 14, 15, 16)

        fun generate(heroId: Int, dataType: EnumConstants.DataType): ServerHeroDetails {
            val random = Random.Default
            val rarity = WeightedRandom(dropRateNew).random(random)
            return generateByRarity(heroId, dataType, rarity)
        }

        fun generateWithoutNewRarity(heroId: Int, dataType: EnumConstants.DataType): ServerHeroDetails {
            val random = Random.Default
            val rarity = WeightedRandom(dropRate).random(random)
            return generateByRarity(heroId, dataType, rarity)
        }

        fun generateByRarity(heroId: Int, dataType: EnumConstants.DataType, rarity: Int): ServerHeroDetails {
            val random = Random.Default
            val rarityStat = rarityStats[rarity]!!
            val bombPower = random.nextInt(rarityStat.bombPower.min, rarityStat.bombPower.max)
            val bombRange = rarityStat.bombRange
            val stamina = random.nextInt(rarityStat.stamina.min, rarityStat.stamina.max)
            val speed = random.nextInt(rarityStat.speed.min, rarityStat.speed.max)
            val bombCount = rarityStat.bombCount
            val abilityList = randomizeAbilities(rarity, random)
            val skin = skinArr[random.nextInt(skinArr.size - 1)]
            val color = random.nextInt(1, COLOR_COUNT)
            val bombSkin = random.nextInt(1, BOMB_SKIN_COUNT)
            
            val heroType = when (dataType) {
                EnumConstants.DataType.TON -> HeroType.TON
                EnumConstants.DataType.RON -> HeroType.RON
                EnumConstants.DataType.VIC -> HeroType.VIC
                EnumConstants.DataType.BAS -> HeroType.BAS
                else -> HeroType.SOL
            }

            return ServerHeroDetails(
                heroId, dataType, bombPower, bombRange, stamina, speed, bombCount, abilityList, skin,
                color, rarity, bombSkin, heroType
            )
        }

        private fun randomizeAbilities(rarity: Int, random: Random): HeroAbilityList {
            val amount = rarityStats[rarity]!!.ability
            val selectedAbilities = mutableListOf<Int>()
            val availableAbilities = abilityIds.toMutableList()
            var availableAbilitiesRate = abilityRate.toMutableList()
            if (rarity == 0) {
                availableAbilitiesRate[2] = 0f
            } else if (rarity > 5) {
                // hero rarity mới, mới có skill mới
                availableAbilitiesRate = abilityRateNew.toMutableList()
            }

            repeat(amount) {
                val index = WeightedRandom(availableAbilitiesRate).random(random)
                selectedAbilities.add(availableAbilities[index])
                availableAbilities.removeAt(index)
                availableAbilitiesRate.removeAt(index)
            }
            return HeroAbilityList(selectedAbilities)
        }
    }

    constructor(sfsObject: ISFSObject) : this(
        _heroId = sfsObject.getLong("bomber_id").toInt(),
        dataType = EnumConstants.DataType.valueOf(sfsObject.getUtfString("data_type")),
        bombPower = sfsObject.getInt("power"),
        bombRange = sfsObject.getInt("bomb_range"),
        stamina = sfsObject.getInt("stamina"),
        speed = sfsObject.getInt("speed"),
        bombCount = sfsObject.getInt("bomb"),
        abilityList = HeroAbilityList(Json.decodeFromString<List<Int>>(sfsObject.getUtfString("ability"))),
        skin = sfsObject.getInt("charactor"),
        color = sfsObject.getInt("color"),
        rarity = sfsObject.getInt("rare"),
        bombSkin = sfsObject.getInt("bomb_skin"),
        type = HeroType.valueOf(sfsObject.getInt("type"))
    )

    override fun isEqualTo(other: IHeroDetails): Boolean {
        return heroId == other.heroId
    }

    override fun isHeroS(): Boolean {
        return true
    }

    fun setHeroId(id: Int) {
        _heroId = id
    }
}