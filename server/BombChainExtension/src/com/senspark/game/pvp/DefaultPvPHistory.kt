package com.senspark.game.pvp

import com.senspark.common.constant.PlayPvPMatchResult
import com.senspark.common.data.LogPlayPvPData
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import java.time.Instant

private fun List<LogPlayPvPData>.getCompletedPlay(): LogPlayPvPData? {
    return firstOrNull { data -> data.matchResult == PlayPvPMatchResult.Complete }
}

private fun LogPlayPvPData.getOpponents(walletAddress: String): List<String> {
    return users.filter { it.walletAddress != walletAddress }.map { it.walletAddress }
}
private fun List<LogPlayPvPData>.getPlayDuration(): Int {
    return sumOf { it.playDuration }
}

private fun List<LogPlayPvPData>.getPlayTime(): Instant {
    return minBy { it.playTime }.playTime
}

class PvPHistoryData(
    val matchId: String,
    val opponentName: String?,
    val opponentWalletAddress: String,
    val playDuration: Int,
    val playTime: Long,
    val win: Boolean
)

class DefaultPvPHistory(
    private val _walletAddress: String,
    private val _log: (String?) -> Unit
) : IPvPHistory {
    private val _data = mutableListOf<PvPHistoryData>()

    override fun addHistory(matchId: String, data: List<LogPlayPvPData>) {
        val completedPlay = data.getCompletedPlay() ?: return
        _data.add(
            PvPHistoryData(
                matchId,
                completedPlay.users[0].opponentName,
                completedPlay.getOpponents(_walletAddress).first(),
                data.getPlayDuration(),
                data.getPlayTime().toEpochMilli(),
                completedPlay.users[0].walletAddress == _walletAddress
            )
        )
    }

    override fun clear() {
        _data.clear()
    }

    private fun getHistories(at: Int, count: Int): List<PvPHistoryData> {
        return _data.drop(at).take(count)
    }

    override fun setItems(items: List<LogPlayPvPData>) {
        _log("[DefaultPvPHistory:init] items: ${items.joinToString(", ") { it.matchId }}")
        val group = items.groupBy { it.matchId }.filter { it.key.split(":").count() > 1 }
        _log("[DefaultPvPHistory:init] group: ${group.keys.joinToString(", ")}")
        group.forEach { (matchId, data) -> addHistory(matchId, data) }
        _data.sortByDescending { it.playTime }
    }

    override fun toSFSArray(at: Int, count: Int): ISFSArray {
        val result = SFSArray()
        getHistories(at, count).forEach {
            val obj = SFSObject()
            obj.putUtfString("matchId", it.matchId)
            if (it.opponentName == null) obj.putNull("opponentName") else obj.putUtfString("opponentName", it.opponentName)
            obj.putUtfString("opponent", it.opponentWalletAddress)
            obj.putInt("time", it.playDuration)
            obj.putLong("date", it.playTime)
            obj.putBool("win", it.win)
            result.addSFSObject(obj)
        }
        return result
    }
}