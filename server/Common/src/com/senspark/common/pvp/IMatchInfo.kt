package com.senspark.common.pvp

interface IMatchInfo {
    /** Match data. */
    val id: String
    val serverId: String
    val serverDetail: String
    val timestamp: Long
    val mode: PvpMode

    /** Match rule. */
    val rule: IMatchRuleInfo

    val team: List<IMatchTeamInfo>

    /** User slot. */
    val slot: Int

    /** All user info. */
    val info: List<IMatchUserInfo>

    /** Used for verification. */
    val hash: String
}