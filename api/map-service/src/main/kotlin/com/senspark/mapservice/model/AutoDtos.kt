package com.senspark.mapservice.model

import kotlinx.serialization.Serializable

// Server-driven treasure mode ("auto play"): MapService walks, plants and explodes for every hero
// and streams the result as TreasureEventBatch entries. Keep in sync with bombcrypto-server-v2's MapServiceDtos.kt.

@Serializable
data class AutoHeroDto(
    val hero: HeroSnapshotDto,
    // Raw speed stat == tiles per second, exactly what the client's BotMove uses.
    val speed: Int,
    val bombCount: Int,
    val blockPass: Boolean = false,
)

@Serializable
data class AutoStartRequest(
    val heroes: List<AutoHeroDto>,
    val fuseMs: Long? = null,
    // Heroes stand still this long after a new map, so the client can show its map-clear UI.
    val mapResetPauseMs: Long? = null,
    // Game server's pause flag, so a resync never un-pauses a paused player.
    val paused: Boolean = false,
)

@Serializable
data class AutoPauseRequest(val paused: Boolean)

@Serializable
data class AutoHeroesRequest(
    val upsert: List<AutoHeroDto> = emptyList(),
    val remove: List<Int> = emptyList(),
    val reason: String = "removed",
)

@Serializable
data class HeroPositionDto(val heroId: Int, val i: Int, val j: Int)

@Serializable
data class AutoBombDto(
    val heroId: Int,
    val bombNo: Int,
    val i: Int,
    val j: Int,
    val plantedAtMs: Long,
    val explodeAtMs: Long,
)

// Everything a (re)connecting client needs; events with seq <= [seq] are already reflected here.
@Serializable
data class AutoSnapshotDto(
    val seq: Long,
    val serverTimeMs: Long,
    val fuseMs: Long,
    val heroes: List<HeroPositionDto>,
    val bombs: List<AutoBombDto>,
    val blocks: List<BlockDto>,
    val awaitingNewMap: Boolean = false,
    val resumeAtMs: Long = 0,
    val paused: Boolean = false,
)

@Serializable
data class AutoSeqResponse(val seq: Long)

@Serializable
data class AutoKeepaliveResponse(val running: Boolean, val seq: Long, val paused: Boolean = false)

object TreasureEventType {
    const val MOVE = "MOVE"
    const val PLANT = "PLANT"
    const val EXPLODE = "EXPLODE"
    const val HERO_JOIN = "HERO_JOIN"
    const val HERO_LEAVE = "HERO_LEAVE"
    const val NEW_MAP = "NEW_MAP"
}

// One simulation step. Flat on purpose (one shape for every [type]); fields a type doesn't use are null.
@Serializable
data class TreasureEventDto(
    val seq: Long,
    val type: String,
    val atMs: Long,
    val heroId: Int? = null,
    // MOVE: from (i, j) the hero walks [path] one tile per [stepMs]; an empty path means "stop here".
    val i: Int? = null,
    val j: Int? = null,
    val path: List<CellDto>? = null,
    val stepMs: Long? = null,
    // MOVE with an empty path: the client roams the hero by itself; 0 = open-ended, else back on (i, j) by then.
    val roamUntilMs: Long? = null,
    // PLANT / EXPLODE
    val bombNo: Int? = null,
    val plantedAtMs: Long? = null,
    val explodeAtMs: Long? = null,
    val takeResult: String? = null,
    val blocksHit: List<BlockHitDto>? = null,
    val mapNowEmpty: Boolean? = null,
    // NEW_MAP
    val heroes: List<HeroPositionDto>? = null,
    val resumeAtMs: Long? = null,
    // HERO_LEAVE
    val reason: String? = null,
)

@Serializable
data class TreasureEventBatch(
    val sessionKey: String,
    val events: List<TreasureEventDto>,
)
