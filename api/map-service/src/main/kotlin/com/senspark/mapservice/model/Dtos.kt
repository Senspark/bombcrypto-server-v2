package com.senspark.mapservice.model

import com.senspark.mapservice.domain.BlockExplodeResult
import kotlinx.serialization.Serializable

/**
 * Wire shape for one map block. Field names/types mirror BombChainExtension's
 * `com.senspark.game.data.model.user.BlockMap` (i, j, type, hp, maxHp) so the main server can
 * serialize its own BlockMap objects straight into this without remapping.
 */
@Serializable
data class BlockDto(
    val i: Int,
    val j: Int,
    val type: Int,
    val hp: Int,
    val maxHp: Int,
)

@Serializable
data class RewardEntryDto(
    val type: String,
    val weight: Int,
    val minValue: Float,
    val maxValue: Float,
)

/**
 * Reward-calculation config the main server owns (DB-backed on that side). `rewardTables` is keyed
 * `"$dataType-$blockType"`, mirroring `BlockRewardDataManager`'s internal key shape.
 * `minStakeHeroConfig` is keyed by rarity as a string (JSON object keys are always strings).
 */
@Serializable
data class RewardConfigDto(
    val rewardTables: Map<String, List<RewardEntryDto>> = emptyMap(),
    val minStakeBcoinTHV1: List<Int> = emptyList(),
    val minStakeSenTHV1: List<Int> = emptyList(),
    val minStakeHeroConfig: Map<String, Int> = emptyMap(),
)

@Serializable
data class ConfigUpdateRequest(
    val rewardConfig: RewardConfigDto? = null,
)

@Serializable
data class MapInitRequest(
    val blocks: List<BlockDto>,
    val tileset: Int = 0,
    val mode: String = "PVE_V2",
    val rewardConfig: RewardConfigDto? = null,
)

@Serializable
data class MapReplaceRequest(
    val blocks: List<BlockDto>,
    val tileset: Int = 0,
    val mode: String = "PVE_V2",
)

@Serializable
data class CellDto(
    val i: Int,
    val j: Int,
)

/**
 * Combat/economy snapshot of the hero's authoritative state, since MapService never has access to
 * the live Hero object. `damageTreasure`/`damageJail`/`totalPower` are the pre-computed stats from
 * `Hero.damageTreasure`/`damageJail`/`totalPower` -- MapService only adds them together, exactly as
 * `UserBlockMapManagerImpl.startExplodeLegacy/Ton` do (`damTreasure = damageTreasure + totalPower`).
 */
@Serializable
data class HeroSnapshotDto(
    val heroId: Int,
    val bombRange: Int,
    val pierceBlock: Boolean,
    val damageTreasure: Int,
    val damageJail: Int,
    val totalPower: Int,
    val stakeBcoin: Double,
    val stakeSen: Double,
    val rarity: Int,
    val isHeroS: Boolean,
    /** The exact `DataType` enum name the main server uses (e.g. "TR", "TON", "SOL") -- used
     * verbatim to build the `"$dataType-$blockType"` reward-table lookup key, matching
     * `BlockRewardDataManager`'s key shape exactly. */
    val dataType: String,
    val isAirdropUser: Boolean,
)

@Serializable
data class RewardHitDto(val type: String, val value: Float)

@Serializable
data class BlockHitDto(
    val i: Int,
    val j: Int,
    val hp: Int,
    /** GameConstants.BlockType code; not part of the original client wire shape, only carried so
     * the caller can derive isDamTreasure/pool-eligibility without a second lookup. */
    val type: Int,
    /** Empty unless this hit brought the block to hp<=0 -- matches the original per-block
     * `Rewards` array attached to each destroyed block's own result object (never an aggregate). */
    val rewards: List<RewardHitDto> = emptyList(),
)

fun BlockExplodeResult.toDto() = BlockHitDto(i, j, hp, type, rewards.map { (type, value) -> RewardHitDto(type, value) })

@Serializable
data class ErrorResponse(
    val error: String,
)
