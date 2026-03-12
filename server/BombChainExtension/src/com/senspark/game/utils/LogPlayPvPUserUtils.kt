package com.senspark.game.utils

import com.senspark.common.constant.PlayPvPLoseReason
import com.senspark.common.data.LogPlayPvPUserData
import java.sql.ResultSet

object LogPlayPvPUserUtils {
    fun fromResultSet(data: ResultSet, userIndex: Int): LogPlayPvPUserData {
        return LogPlayPvPUserData(
            data.getString("user_name_$userIndex"),
            data.getInt("bomber_id_$userIndex"),
            data.getInt("item_collect_$userIndex"),
            try {
                PlayPvPLoseReason.fromString(data.getString("reason_lose_$userIndex"))
            } catch (ex: Exception) {
                PlayPvPLoseReason.Null
            },
            data.getString("opponentName")
        )
    }
}