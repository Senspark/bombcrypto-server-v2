package com.senspark.game.manager.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class MiscConfigs(
    @SerialName("pvp_match_count")
    private var _pvpMatchCount: Int = 0,
    @SerialName("win_count")
    private var _pvpMatchWinCount: Int = 0,
    @SerialName("last_receive_offline_reward")
    override var lastReceiveOfflineReward: Long = 0,
    @SerialName("win_streaks")
    private var _winStreaks: Long = 0,
    @SerialName("lose_streaks")
    private var _loseStreaks: Long = 0
) : IMiscConfigs {

    constructor(isWin: Boolean) : this(
        1,
        if (isWin) 1 else 0,
        if (isWin) 1 else 0,
        if (!isWin) 1 else 0,
    )

    override val pvpMatchCount get() = _pvpMatchCount

    override fun inCreatePvpMatchCount(isWin: Boolean) {
        _pvpMatchCount++
        if (isWin) {
            _pvpMatchWinCount++
            _winStreaks++
            _loseStreaks = 0
        } else {
            _winStreaks = 0
            _loseStreaks++
        }
    }
} 