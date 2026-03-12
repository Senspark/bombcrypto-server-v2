package com.senspark.lib.utils

import java.time.Instant
import java.time.temporal.ChronoUnit

object TimeUtils {

    fun isSameDate(first: Instant, second: Instant = Instant.now()): Boolean {
        return first.truncatedTo(ChronoUnit.DAYS) == second.truncatedTo(ChronoUnit.DAYS)
    }
} 