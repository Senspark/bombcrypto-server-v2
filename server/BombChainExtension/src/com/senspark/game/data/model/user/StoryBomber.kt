package com.senspark.game.data.model.user

import kotlinx.serialization.Serializable

@Serializable
class StoryBomber {
    var bomberId = -1
    var lastPlayedTimestamp = 0L

    fun getRemainingTime(): Long {
        return lastPlayedTimestamp + 86400000 - System.currentTimeMillis()
    }
}