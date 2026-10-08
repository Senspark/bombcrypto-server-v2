package com.senspark.game.manager.blockMap.mapservice

import kotlinx.serialization.Serializable

// Wire contract with MapService: keep in sync with its model/Dtos.kt and AutoDtos.kt (no shared module).

@Serializable
data class MsBlockDto(val i: Int, val j: Int, val type: Int, val hp: Int, val maxHp: Int)

@Serializable
data class MsRewardEntryDto(val type: String, val weight: Int, val minValue: Float, val maxValue: Float)

@Serializable
data class MsRewardConfigDto(
    // Keyed "$dataType-$blockType".
    val rewardTables: Map<String, List<MsRewardEntryDto>> = emptyMap(),
    val minStakeBcoinTHV1: List<Int> = emptyList(),
    val minStakeSenTHV1: List<Int> = emptyList(),
    // Keyed by rarity as string.
    val minStakeHeroConfig: Map<String, Int> = emptyMap(),
)

@Serializable
data class MsMapInitRequest(
    val blocks: List<MsBlockDto>,
    val tileset: Int,
    val mode: String,
    val rewardConfig: MsRewardConfigDto? = null,
)

@Serializable
data class MsMapReplaceRequest(
    val blocks: List<MsBlockDto>,
    val tileset: Int,
    val mode: String,
)

@Serializable
data class MsCellDto(val i: Int, val j: Int)

// Hero snapshot for MapService; [dataType] is the DataType enum name used in reward-table keys.
@Serializable
data class MsHeroSnapshotDto(
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
    val dataType: String,
    val isAirdropUser: Boolean,
)

@Serializable
data class MsRewardHitDto(val type: String, val value: Float)

@Serializable
data class MsBlockHitDto(
    val i: Int,
    val j: Int,
    val hp: Int,
    val type: Int,
    // Empty unless this hit destroyed the block.
    val rewards: List<MsRewardHitDto> = emptyList(),
)

// ================== Server-driven treasure mode (MapService "auto play") ==================

@Serializable
data class MsAutoHeroDto(
    val hero: MsHeroSnapshotDto,
    // Raw speed stat == tiles per second.
    val speed: Int,
    val bombCount: Int,
    val blockPass: Boolean = false,
)

@Serializable
data class MsAutoStartRequest(
    val heroes: List<MsAutoHeroDto>,
    val fuseMs: Long? = null,
    val mapResetPauseMs: Long? = null,
    val paused: Boolean = false,
)

@Serializable
data class MsAutoPauseRequest(val paused: Boolean)

@Serializable
data class MsAutoHeroesRequest(
    val upsert: List<MsAutoHeroDto> = emptyList(),
    val remove: List<Int> = emptyList(),
    val reason: String = "removed",
)

@Serializable
data class MsHeroPositionDto(val heroId: Int, val i: Int, val j: Int)

@Serializable
data class MsAutoBombDto(
    val heroId: Int,
    val bombNo: Int,
    val i: Int,
    val j: Int,
    val plantedAtMs: Long,
    val explodeAtMs: Long,
)

// Events with seq <= [seq] are already reflected here.
@Serializable
data class MsAutoSnapshotDto(
    val seq: Long,
    val serverTimeMs: Long,
    val fuseMs: Long,
    val heroes: List<MsHeroPositionDto> = emptyList(),
    val bombs: List<MsAutoBombDto> = emptyList(),
    val blocks: List<MsBlockDto> = emptyList(),
    val awaitingNewMap: Boolean = false,
    val resumeAtMs: Long = 0,
    val paused: Boolean = false,
)

@Serializable
data class MsAutoKeepaliveResponse(val running: Boolean, val seq: Long, val paused: Boolean = false)

object MsTreasureEventType {
    const val MOVE = "MOVE"
    const val PLANT = "PLANT"
    const val EXPLODE = "EXPLODE"
    const val HERO_JOIN = "HERO_JOIN"
    const val HERO_LEAVE = "HERO_LEAVE"
    const val NEW_MAP = "NEW_MAP"
}

// Flat on purpose: each [type] fills only its own fields.
@Serializable
data class MsTreasureEventDto(
    val seq: Long,
    val type: String,
    val atMs: Long,
    val heroId: Int? = null,
    val i: Int? = null,
    val j: Int? = null,
    val path: List<MsCellDto>? = null,
    val stepMs: Long? = null,
    val roamUntilMs: Long? = null,
    val bombNo: Int? = null,
    val plantedAtMs: Long? = null,
    val explodeAtMs: Long? = null,
    val takeResult: String? = null,
    val blocksHit: List<MsBlockHitDto>? = null,
    val mapNowEmpty: Boolean? = null,
    val heroes: List<MsHeroPositionDto>? = null,
    val resumeAtMs: Long? = null,
    val reason: String? = null,
)

// One AP_MAP_TREASURE_EVENT_CHANNEL message.
@Serializable
data class MsTreasureEventBatch(
    val sessionKey: String,
    val events: List<MsTreasureEventDto>,
)
