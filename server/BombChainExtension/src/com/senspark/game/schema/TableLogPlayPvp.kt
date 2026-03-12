package com.senspark.game.schema

import com.senspark.game.schema.utils.floatArray
import com.senspark.game.schema.utils.integerArray
import com.senspark.game.schema.utils.textArray
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object TableLogPlayPvp : Table("log_play_pvp") {
    val matchId = varchar("id_match", 50)
    val serverId = varchar("server_id", 20)
    val matchResult = varchar("match_result", 42)
    val matchTimestamp = timestamp("play_date")
    val matchDuration = integer("time_match")
    val username1 = varchar("user_name_1", 42)
    val username2 = varchar("user_name_2", 42)
    val heroId1 = integer("bomber_id_1")
    val heroId2 = integer("bomber_id_2")
    val collectedItem1 = integer("item_collect_1")
    val collectedItem2 = integer("item_collect_2")
    val loseReason1 = varchar("reason_lose_1", 42)
    val loseReason2 = varchar("reason_lose_2", 42)
    val deltaPoint1 = integer("point_1")
    val deltaPoint2 = integer("point_2")
    val latency = integerArray("latency")
    val timeDelta = integerArray("time_delta")
    val lossRate = floatArray("loss_rate")
    val countryCode = textArray("country_code")
}