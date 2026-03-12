package com.senspark.game.schema

import org.jetbrains.exposed.sql.Table

object TableLogRewardDeposit : Table("log_reward_deposit") {
    val userName = char("user_name", 42)
    val rewardType = varchar("reward_type", 30)
    val rewardAmount = float("reward_amount")
}