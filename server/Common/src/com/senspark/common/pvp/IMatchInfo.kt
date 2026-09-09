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

    val gameMode: Int
    val wagerMode: Int
    val wagerTier: Int
    val wagerToken: Int

    /** Used for verification. */
    val hash: String
}