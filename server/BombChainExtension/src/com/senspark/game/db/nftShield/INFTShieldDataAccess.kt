package com.senspark.game.db.nftShield

import com.senspark.common.IDataAccess

interface INFTShieldDataAccess : IDataAccess {
    fun getPinHash(uid: Int): String?
    fun setupPin(uid: Int, pinHash: String)
    fun recordFailedAttempt(uid: Int, maxAttempts: Int, lockDurationSecs: Long): Boolean
    fun resetFailedAttempts(uid: Int)
    fun isLocked(uid: Int): Boolean
    fun getFailedAttempts(uid: Int): Int
    fun getLockUntil(uid: Int): Long?
}
