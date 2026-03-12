package com.senspark.game.pvp.manager

import java.time.Instant

class EpochTimeManager : ITimeManager {
    override val timestamp get() = Instant.now().toEpochMilli()
}