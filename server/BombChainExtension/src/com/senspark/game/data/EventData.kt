package com.senspark.game.data

class EventData(
    val startTime: Long,
    val endTime: Long
) {
    companion object {
        val `null` = EventData(0, 0)
    }
}