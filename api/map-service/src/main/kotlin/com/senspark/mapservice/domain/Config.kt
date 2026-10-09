package com.senspark.mapservice.domain

import com.senspark.mapservice.model.RewardConfigDto
import com.senspark.mapservice.model.RewardEntryDto

data class RewardEntry(
    val type: String,
    val weight: Int,
    val minValue: Float,
    val maxValue: Float,
)

/**
 * Reward-roll config, ported from what `BlockRewardDataManager`/`IGameConfigManager` hold on the
 * main server (DB-backed there). MapService is handed a snapshot of just the fields the reward
 * roll needs -- see `UserBlockMapManagerImpl.getRewards` / `BlockRewardDataManager.getRewards`.
 */
data class RewardConfig(
    val rewardTables: Map<String, List<RewardEntry>> = emptyMap(),
    val minStakeBcoinTHV1: List<Int> = emptyList(),
    val minStakeSenTHV1: List<Int> = emptyList(),
    val minStakeHeroConfig: Map<Int, Int> = emptyMap(),
) {
    companion object {
        fun from(dto: RewardConfigDto): RewardConfig {
            return RewardConfig(
                rewardTables = dto.rewardTables.mapValues { (_, entries) -> entries.map { it.toEntry() } },
                minStakeBcoinTHV1 = dto.minStakeBcoinTHV1,
                minStakeSenTHV1 = dto.minStakeSenTHV1,
                minStakeHeroConfig = dto.minStakeHeroConfig.mapKeys { (k, _) -> k.toInt() },
            )
        }
    }
}

private fun RewardEntryDto.toEntry() = RewardEntry(type, weight, minValue, maxValue)

data class SessionConfig(
    val reward: RewardConfig = RewardConfig(),
)

/**
 * Process-wide default config, updated via `POST /config` whenever the main server's own
 * `IBlockRewardDataManager` reloads. New sessions default to whatever this
 * last received; a session's own `/init` body can override it explicitly.
 */
object GlobalConfigStore {
    @Volatile
    var current: SessionConfig = SessionConfig()
        private set

    fun updateReward(reward: RewardConfig) {
        current = current.copy(reward = reward)
    }
}
