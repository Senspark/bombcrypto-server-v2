package com.senspark.game.db.model

import java.sql.ResultSet

class UserAirdropClaimed(
    val airdropCode: String,
    val claimDate: Long,
    val isCompleted: Boolean
) {
    companion object {
        fun fromResultSet(rs: ResultSet): UserAirdropClaimed {
            return UserAirdropClaimed(
                rs.getString("airdrop_code"),
                rs.getTimestamp("claim_date").time,
                rs.getInt("is_completed") == 1
            )
        }
    }
}