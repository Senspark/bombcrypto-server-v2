package com.senspark.game.data.manager.block

import com.senspark.game.data.model.config.IBlockReward
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.ErrorCode
import com.senspark.game.exception.CustomException
import com.senspark.game.utils.WeightedRandom
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.random.Random

class BlockRewardDataManager(
    private val _shopDataAccess: IShopDataAccess,
) : IBlockRewardDataManager {
    private var _blockRewards: HashMap<String, List<IBlockReward>> = hashMapOf()
    private var _zeroSums: HashSet<String> = hashSetOf()
    private val _random = Random.Default
    private val _lock = ReentrantReadWriteLock()

    override fun initialize() {
        setConfig(_shopDataAccess.loadBlockReward())
    }

    /**
     * Maybe consider thread safety
     */
    override fun setConfig(blockRewards: HashMap<DataType, HashMap<Int, MutableList<IBlockReward>>>) {
        val newBlockRewards: HashMap<String, List<IBlockReward>> = hashMapOf()
        val newZeroSums: HashSet<String> = hashSetOf()

        for ((dataType, map) in blockRewards) {
            for ((blockType, rewards) in map) {
                val k = "$dataType-$blockType"
                newBlockRewards[k] = rewards
                val zeroSum = rewards.sumOf { it.weight } == 0
                if (zeroSum) {
                    newZeroSums.add(k)
                }
            }
        }
        _lock.write {
            _blockRewards = newBlockRewards
            _zeroSums = newZeroSums
        }
    }

    override fun getRewards(
        dataType: DataType,
        rewardTypeRandom: List<BLOCK_REWARD_TYPE>,
        blockType: Int
    ): List<IBlockReward> {
        _lock.read {
            val k = "$dataType-$blockType"
            if (!_blockRewards.containsKey(k) || _blockRewards[k] == null) {
                throw CustomException("Reward $dataType, $blockType invalid", ErrorCode.SERVER_ERROR)
            }
            if (_zeroSums.contains(k)) {
                return listOf()
            }
            val rewards = _blockRewards[k]!!.filter { rewardTypeRandom.contains(it.type) }

            // rewards đã được xếp theo thứ tự weight tăng dần.
            // Hiện tại chỉ return về 1 reward (nhưng vẫn dùng List)

            val randomizer = WeightedRandom(rewards.map { it.weight.toFloat() })
            val rId = randomizer.random(_random)
            if (rId < 0 || rId >= rewards.size) {
                throw CustomException("[GET_BLOCK_REWARD] Invalid rId $rId", ErrorCode.SERVER_ERROR)
            }
            return listOf(rewards[rId])
        }
    }

    override fun dumpRewards(): String {
        _lock.read {
            val sb = StringBuilder()
            for ((k, v) in _blockRewards) {
                sb.append("$k: ")
                for (reward in v) {
                    sb.append(reward.dump())
                    sb.append(", ")
                }
                sb.append("\n")
            }
            return sb.toString()
        }
    }
}