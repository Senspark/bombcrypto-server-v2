package com.senspark.game.manager.blockMap.mapservice

import kotlinx.serialization.Serializable

// Wire contract with MapService: keep in sync with its model/Dtos.kt (no shared module).

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
data class MsGameConfigDto(
    val isCheckPlantMoveSpeed: Boolean = true,
    val isRejectPlantTooFast: Boolean = true,
    val plantMoveSpeedToleranceMs: Int = 1000,
    val plantMoveSpeedMultiplier: Float = 1.15f,
    val plantMoveSpeedMin: Float = 1.0f,
)

@Serializable
data class MsMapInitRequest(
    val blocks: List<MsBlockDto>,
    val tileset: Int,
    val mode: String,
    val rewardConfig: MsRewardConfigDto? = null,
    val gameConfig: MsGameConfigDto? = null,
)

@Serializable
data class MsMapReplaceRequest(
    val blocks: List<MsBlockDto>,
    val tileset: Int,
    val mode: String,
)

@Serializable
data class MsTargetHeroRequest(
    val heroId: Int,
    val seedI: Int? = null,
    val seedJ: Int? = null,
    val rejectI: Int? = null,
    val rejectJ: Int? = null,
)

@Serializable
data class MsTargetsRequest(val heroes: List<MsTargetHeroRequest>)

@Serializable
data class MsHeroTargetDto(val heroId: Int, val i: Int, val j: Int)

@Serializable
data class MsTargetsResponse(val targets: List<MsHeroTargetDto> = emptyList())

@Serializable
data class MsPlantRequest(
    val heroId: Int,
    val bombNo: Int,
    val i: Int,
    val j: Int,
    val speed: Int,
    val bombCount: Int,
    // When set, MapService arms the fuse and publishes MsExplodeResultEvent; don't call /explode then.
    val hero: MsHeroSnapshotDto? = null,
    val fuseMs: Long? = null,
)

@Serializable
data class MsCellDto(val i: Int, val j: Int)

@Serializable
data class MsPlantResponse(
    // PlantBombResult name.
    val result: String,
    val nextTarget: MsCellDto? = null,
    // Set even if isRejectPlantTooFast didn't reject; caller decides to kick.
    val isPlantTooFastHackFlag: Boolean = false,
    val fuseArmed: Boolean = false,
)

// One AP_MAP_EXPLODE_RESULT_STR entry; [sessionKey] routes it to the owning manager.
@Serializable
data class MsExplodeResultEvent(
    val sessionKey: String,
    val heroId: Int,
    val bombNo: Int,
    val i: Int,
    val j: Int,
    // OK | ALREADY_TAKEN | CANNOT_SET_BOOM
    val takeResult: String,
    val blocksHit: List<MsBlockHitDto> = emptyList(),
    val mapNowEmpty: Boolean = false,
    val plantedAtMs: Long = 0,
    val explodedAtMs: Long = 0,
)

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
data class MsExplodeRequest(
    val bombNo: Int,
    val i: Int,
    val j: Int,
    val hero: MsHeroSnapshotDto,
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

@Serializable
data class MsExplodeResponse(
    val takeResult: String,
    val blocksHit: List<MsBlockHitDto> = emptyList(),
    val mapNowEmpty: Boolean = false,
)
