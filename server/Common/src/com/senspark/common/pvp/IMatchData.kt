package com.senspark.common.pvp

interface IMatchData {
    /** The id of the game. */
    val id: String

    var status: MatchStatus

    var observerCount: Int

    /** When the match started. */
    val startTimestamp: Long

    /** When the ready phase starts. */
    var readyStartTimestamp: Long

    /** When the round started, in milliseconds since epoch. */
    var roundStartTimestamp: Long

    /** Current round, zero-indexed. */
    var round: Int

    /** Team scores, map from team ID to score. */
    val results: MutableList<IMatchResultInfo>
}

/**
 * Match phase:
 * - match start
 * - match ready start
 * - match ready finish
 * - match round start
 * - match round finish
 * - match finish
 */
enum class MatchStatus {
    /**
     * Round ready phase.
     */
    Ready,

    /** Round start phase. */
    Started,

    /** Round finish phase. */
    Finished,

    /** Match started. */
    MatchStarted,

    /** Match finished. */
    MatchFinished,
}