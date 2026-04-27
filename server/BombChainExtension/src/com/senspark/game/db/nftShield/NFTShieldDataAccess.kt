package com.senspark.game.db.nftShield

import com.senspark.common.IDatabase
import com.senspark.common.utils.ILogger
import com.senspark.lib.db.BaseDataAccess
import java.sql.Timestamp
import java.time.Instant

class NFTShieldDataAccess(
    database: IDatabase,
    enableSqlLog: Boolean,
    logger: ILogger,
) : BaseDataAccess(database, enableSqlLog, logger), INFTShieldDataAccess {

    override fun initialize() {}

    override fun getPinHash(uid: Int): String? {
        val statement = "SELECT pin_hash FROM user_nft_shield WHERE uid = ?"
        var hash: String? = null
        executeQuery(statement, arrayOf(uid)) {
            hash = it.getString("pin_hash")
        }
        return hash
    }

    override fun setupPin(uid: Int, pinHash: String) {
        val statement = """
            INSERT INTO user_nft_shield (uid, pin_hash, failed_attempts, lock_until, updated_at) 
            VALUES (?, ?, 0, NULL, CURRENT_TIMESTAMP) 
            ON CONFLICT (uid) DO UPDATE 
            SET pin_hash = excluded.pin_hash, failed_attempts = 0, lock_until = NULL, updated_at = CURRENT_TIMESTAMP
        """.trimIndent()
        executeUpdateThrowException(statement, arrayOf(uid, pinHash))
    }

    override fun recordFailedAttempt(uid: Int, maxAttempts: Int, lockDurationSecs: Long): Boolean {
        val statement = """
            UPDATE user_nft_shield 
            SET failed_attempts = failed_attempts + 1,
                lock_until = CASE 
                    WHEN failed_attempts + 1 >= ? THEN CURRENT_TIMESTAMP + (? || ' seconds')::interval 
                    ELSE lock_until 
                END,
                updated_at = CURRENT_TIMESTAMP
            WHERE uid = ?
            RETURNING failed_attempts
        """.trimIndent()
        
        var isNowLocked = false
        val sfsArray = database.createQueryBuilder(log).addStatement(statement, arrayOf(maxAttempts, lockDurationSecs, uid)).executeQuery()
        if (sfsArray.size() > 0) {
            val attempts = sfsArray.getSFSObject(0).getInt("failed_attempts")
            if (attempts >= maxAttempts) {
                isNowLocked = true
            }
        }
        return isNowLocked
    }

    override fun resetFailedAttempts(uid: Int) {
        val statement = "UPDATE user_nft_shield SET failed_attempts = 0, lock_until = NULL, updated_at = CURRENT_TIMESTAMP WHERE uid = ?"
        executeUpdateThrowException(statement, arrayOf(uid))
    }

    override fun isLocked(uid: Int): Boolean {
        val statement = "SELECT lock_until FROM user_nft_shield WHERE uid = ?"
        var locked = false
        executeQuery(statement, arrayOf(uid)) {
            val lockUntilStr = it.getUtfString("lock_until")
            if (lockUntilStr != null) {
                val lockUntil = Timestamp.valueOf(lockUntilStr)
                if (lockUntil.after(Timestamp.from(Instant.now()))) {
                    locked = true
                }
            }
        }
        return locked
    }

    override fun getFailedAttempts(uid: Int): Int {
        val statement = "SELECT failed_attempts FROM user_nft_shield WHERE uid = ?"
        var attempts = 0
        executeQuery(statement, arrayOf(uid)) {
            attempts = it.getInt("failed_attempts")
        }
        return attempts
    }

    override fun getLockUntil(uid: Int): Long? {
        val statement = "SELECT EXTRACT(EPOCH FROM lock_until)::BIGINT AS lock_until_epoch FROM user_nft_shield WHERE uid = ?"
        var lockUntil: Long? = null
        executeQuery(statement, arrayOf(uid)) {
            if (it.containsKey("lock_until_epoch") && !it.isNull("lock_until_epoch")) {
                lockUntil = it.getLong("lock_until_epoch")
            }
        }
        return lockUntil
    }
}
