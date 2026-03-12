package com.senspark.game.utils

import com.senspark.game.data.EventData
import java.time.Instant

fun EventData.isValidate(): Boolean {
    return Instant.now().toEpochMilli() in startTime until endTime
}