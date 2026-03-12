package com.senspark.game.db

import com.senspark.common.IDatabase
import com.senspark.common.utils.ILogger
import com.senspark.game.data.model.config.AirDrop
import com.senspark.game.db.model.UserAirdropClaimed
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.senspark.lib.db.BaseDataAccess

class RewardDataAccessExecutor(
    database: IDatabase,
    log: Boolean,
    logger: ILogger,
) : BaseDataAccess(database, log, logger) {
    fun loadUserAirdropClaimed(uid: Int): HashMap<String, UserAirdropClaimed> {
        val claimed = HashMap<String, UserAirdropClaimed>()
        val statement = """
            SELECT  airdrop_code,
                    UNIX_TIMESTAMP(claim_date) * 1000  AS claim_date,
                    is_completed
            FROM user_airdrop_claimed
            WHERE uid = ?
        """.trimIndent()
        val params = arrayOf<Any?>(uid)
        executeQuery(statement, params) {
            val userAirdropClaimed = UserAirdropClaimed.fromResultSet(it)
            claimed[userAirdropClaimed.airdropCode] = userAirdropClaimed
        }
        return claimed
    }

    fun userClaimAirdrop(
        uid: Int,
        airdrop: AirDrop,
        claimFee: Float,
        feeRewardType: BLOCK_REWARD_TYPE,
        isComplete: Int
    ) {
        val statement = "CALL sp_user_claim_airdrop_v2(?, ?, ?, ?, ?, ?, ?);"
        val params = arrayOf<Any?>(
            uid,
            airdrop.code,
            airdrop.rewardAmount,
            claimFee,
            feeRewardType.name,
            feeRewardType.swapDepositedOrReward().name,
            isComplete
        )
        executeUpdateThrowException(statement, params)
    }

    fun setUserClaimAirdropSuccess(uid: Int, airDropCodes: List<String>) {
        if (airDropCodes.isEmpty()) return
        val statement = """
            UPDATE user_airdrop_claimed
            SET is_completed = 1
            WHERE uid = ?
              AND airdrop_code IN (${airDropCodes.joinToString(",") { "'$it'" }});    
        """.trimIndent()
        val params = arrayOf<Any?>(uid)
        executeUpdateThrowException(statement, params)
    }
}