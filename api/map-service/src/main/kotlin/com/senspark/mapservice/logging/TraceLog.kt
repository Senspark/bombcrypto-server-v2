package com.senspark.mapservice.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

// Session/auto-play request logging; level set by MAP_SERVICE_TRACE_LOG (WARN mutes it for stress tests).
object TraceLog {
    val log: Logger = LoggerFactory.getLogger("com.senspark.mapservice.trace")

    inline fun info(message: () -> String) {
        if (log.isInfoEnabled) log.info(message())
    }
}
