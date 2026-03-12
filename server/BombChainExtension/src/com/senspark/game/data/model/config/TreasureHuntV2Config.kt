package com.senspark.game.data.model.config

import com.senspark.common.utils.toSFSArray
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.json.Json

class TreasureHuntV2Config(
    var data: HashMap<String, String>,
    var rewardPool: MutableMap<Int, Double>
) {
    companion object {
        private const val REWARD_EACH_PERIOD = "reward_each_period"
        private const val PERIOD = "period"
        private const val MAX_POOL = "max_pool"
        private const val MIN_STAKE = "min_stake"
    }

    val period: Int = data[PERIOD]?.toInt() ?: 60
    val maxPool: Int = data[MAX_POOL]?.toInt() ?: 500000
    val minStake: List<Int> = Json.decodeFromString<List<Int>>(data[MIN_STAKE] ?: "[]")
    
    fun rewardPoolToSFSArray(): ISFSArray {
        return rewardPool.toSFSArray { 
            SFSObject().apply {
                putInt("pool_id", it.key)
                putDouble("remaining_reward", it.value)
            }
        } 
    }
}