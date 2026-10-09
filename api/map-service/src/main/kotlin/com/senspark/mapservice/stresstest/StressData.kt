package com.senspark.mapservice.stresstest

import com.senspark.mapservice.domain.GameConstants
import com.senspark.mapservice.model.BlockDto
import com.senspark.mapservice.model.HeroSnapshotDto
import com.senspark.mapservice.model.RewardConfigDto
import com.senspark.mapservice.model.RewardEntryDto
import kotlin.random.Random

/** Generators for synthetic maps/heroes/config -- everything a virtual user needs to look like a
 * real caller to MapService, without any of the DB/game-config plumbing the real server has. */
object StressData {
    val dataTypes = listOf("TR", "TON", "SOL")

    private data class BlockTypeSpec(val type: Int, val weight: Int, val hp: Int)

    // Weighted like the real drop table roughly is: mostly cheap wooden/silver blocks, a handful of
    // rarer high-value ones, plus jail blocks so damageJail gets exercised too.
    private val blockTypeSpecs = listOf(
        BlockTypeSpec(GameConstants.BlockType.NORMAL, 30, 20),
        BlockTypeSpec(GameConstants.BlockType.WOODEN, 35, 40),
        BlockTypeSpec(GameConstants.BlockType.SILVER, 15, 80),
        BlockTypeSpec(GameConstants.BlockType.GOLDEN, 10, 150),
        BlockTypeSpec(GameConstants.BlockType.DIAMOND, 6, 300),
        BlockTypeSpec(GameConstants.BlockType.LEGEND, 2, 500),
        BlockTypeSpec(GameConstants.BlockType.JAIL, 12, 40),
    )
    private val totalBlockWeight = blockTypeSpecs.sumOf { it.weight }

    /** A fresh random map honoring MapGrid's fixed odd/odd wall pattern -- blocks are never placed
     * on a wall cell, exactly like the real server's own `createRandomMap`. */
    fun randomMap(density: Double, random: Random): List<BlockDto> {
        val blocks = mutableListOf<BlockDto>()
        for (i in 0 until GameConstants.MAP_MAX_COL) {
            for (j in 0 until GameConstants.MAP_MAX_ROW) {
                if (i % 2 == 1 && j % 2 == 1) continue
                if (random.nextDouble() >= density) continue
                val spec = blockTypeSpecs[pickBlockTypeIndex(random)]
                blocks.add(BlockDto(i, j, spec.type, spec.hp, spec.hp))
            }
        }
        return blocks
    }

    private fun pickBlockTypeIndex(random: Random): Int {
        var roll = random.nextInt(totalBlockWeight)
        for ((index, spec) in blockTypeSpecs.withIndex()) {
            if (roll < spec.weight) return index
            roll -= spec.weight
        }
        return blockTypeSpecs.lastIndex
    }

    /** Reward table covering every generated block type for [dataType], with all four reward types
     * (COIN/BOMBERMAN always eligible, BCOIN/SENSPARK gated on stake -- see [randomHero]) so the
     * explode reward roll exercises every branch of `Session.rollRewards`. */
    fun rewardConfigFor(dataType: String): RewardConfigDto {
        val tables = blockTypeSpecs.associate { spec ->
            "$dataType-${spec.type}" to listOf(
                RewardEntryDto("COIN", 70, 0.5f, 2.0f),
                RewardEntryDto("BOMBERMAN", 5, 1f, 1f),
                RewardEntryDto("BCOIN", 15, 0.1f, 1.0f),
                RewardEntryDto("SENSPARK", 10, 0.1f, 1.0f),
            )
        }
        // Indexed by rarity; sized past any rarity this tool generates (see --hero-rarity) so a
        // stake-gated reward type (BCOIN/SENSPARK) is reachable at every rarity instead of silently
        // falling back to a 0 threshold via Session.kt's `getOrElse(rarity) { 0 }`.
        return RewardConfigDto(
            rewardTables = tables,
            minStakeBcoinTHV1 = (0..MAX_SUPPORTED_RARITY).map { it * 10 },
            minStakeSenTHV1 = (0..MAX_SUPPORTED_RARITY).map { it * 5 },
            minStakeHeroConfig = (0..MAX_SUPPORTED_RARITY).associate { it.toString() to it * 5 },
        )
    }

    /** Highest hero rarity the generated reward config accounts for; `--hero-rarity` above this
     * still works (Session.kt falls back to a 0 stake threshold), just untested by this tool. */
    const val MAX_SUPPORTED_RARITY = 10

    fun randomHero(heroId: Int, dataType: String, isAirdropUser: Boolean, rarity: Int, random: Random): HeroSnapshotDto {
        return HeroSnapshotDto(
            heroId = heroId,
            bombRange = random.nextInt(1, 4),
            pierceBlock = random.nextDouble() < 0.15,
            damageTreasure = random.nextInt(10, 60),
            damageJail = random.nextInt(10, 60),
            totalPower = random.nextInt(0, 30),
            stakeBcoin = random.nextDouble(0.0, 100.0),
            stakeSen = random.nextDouble(0.0, 50.0),
            rarity = rarity,
            isHeroS = random.nextDouble() < 0.1,
            dataType = dataType,
            isAirdropUser = isAirdropUser,
        )
    }
}
