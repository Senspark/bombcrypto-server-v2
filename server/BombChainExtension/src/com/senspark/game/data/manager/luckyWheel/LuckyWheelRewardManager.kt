package com.senspark.game.data.manager.luckyWheel

import com.senspark.game.constant.ItemType
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.manager.mysteryBox.IMysteryBoxManager
import com.senspark.game.data.model.config.Item
import com.senspark.game.data.model.config.LuckyWheelReward
import com.senspark.game.db.IShopDataAccess
import com.senspark.game.exception.CustomException
import com.senspark.game.utils.random.IWeightedRandom
import com.senspark.game.utils.random.WeightedRandomFloat

class LuckyWheelRewardManager(
    private val _shopDataAccess: IShopDataAccess,
    private val _configItemManager: IConfigItemManager,
    private val _mysteryBoxManager: IMysteryBoxManager,
) : ILuckyWheelRewardManager {

    override val rewards: MutableList<LuckyWheelReward> = mutableListOf()
    private lateinit var _randomManger: IWeightedRandom<LuckyWheelReward>

    override fun initialize() {
        rewards.addAll(_shopDataAccess.loadLuckyWheelReward(_configItemManager))
        _randomManger = WeightedRandomFloat(rewards)
    }

    override fun randomReward(): Pair<LuckyWheelReward, List<Triple<Item, Int, Long>>> {
        if (rewards.isEmpty()) {
            throw CustomException("Reward list is empty now")
        }
        val reward = _randomManger.randomItem()
        return reward.item.let {
            if (it == null) {
                return Pair(reward, emptyList())
            }
            if (it.type == ItemType.MYSTERY_BOX) {
                val mysteryReward = _mysteryBoxManager.getRandomItem()
                Pair(
                    reward,
                    listOf(Triple(mysteryReward.item, mysteryReward.quantity, mysteryReward.expirationAfter ?: 0))
                )
            } else {
                Pair(reward, listOf(Triple(it, reward.quantity, 0)))
            }
        }
    }

}