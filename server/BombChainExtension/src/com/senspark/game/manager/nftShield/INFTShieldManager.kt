package com.senspark.game.manager.nftShield

import com.senspark.common.service.IGlobalService

interface INFTShieldManager : IGlobalService {
    fun getShieldStatus(uid: Int): Boolean
    fun setupPin(uid: Int, pin: String)
    fun verifyPin(uid: Int, pin: String): Boolean
    fun isLocked(uid: Int): Boolean
    fun getLockUntil(uid: Int): Long?
    fun getFailedAttempts(uid: Int): Int
    fun generateSignature(walletAddress: String, nonce: Long, tokenIds: List<Long>): String
}
