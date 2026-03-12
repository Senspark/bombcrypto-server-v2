package com.senspark.game.pvp

import com.senspark.common.data.LogPlayPvPData
import com.smartfoxserver.v2.entities.data.ISFSArray

interface IPvPHistory {
    fun clear()
    fun addHistory(matchId: String, data: List<LogPlayPvPData>)
    fun setItems(items: List<LogPlayPvPData>)
    fun toSFSArray(at: Int, count: Int): ISFSArray
}