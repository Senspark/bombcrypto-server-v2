package com.senspark.game.manager.dailyTask

import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.Serializable

@Serializable
data class CachedTask(
    val date: String,
    val tasks: List<Int>
)

@Serializable
data class DailyTask(
    val id: Int,
    val completed: Int,
    val rewardString: String,
    var reward: List<RewardDailyTask> = emptyList(),
    val isRandom: Boolean,
    val isDefault: Boolean,
    val expired: Long,
    val isDeleted: Boolean
)

@Serializable
data class RewardDailyTask(
    val itemId: Int,
    val quantity: Int
)

data class TodayTask(
    var finalRewardClaimed: Boolean,
    val urlConfig: String,
    var tasks: List<UserTask>
) {
    fun idToString(): String {
        return tasks.joinToString(",") { it.taskId.toString() }
    }

    fun progressToString(): String {
        return tasks.joinToString(",") { it.progress.toString() }
    }

    fun claimedToString(): String {
        return tasks.joinToString(",") { it.claimed.toString() }
    }

    fun rewardToString(): String {
        return tasks.joinToString(",") { task ->
            task.reward.joinToString(",") { item ->
                if (item.expiration != 0L) {
                    "[${item.itemId}:${item.quantity}:${item.expiration}]"
                } else {
                    "[${item.itemId}:${item.quantity}]"
                }
            }
        }
    }

    private fun allTaskToSfsArray(): ISFSArray {
        val sfsArray = SFSArray()
        tasks.forEach {
            val task = SFSObject()
            task.putInt("task_id", it.taskId)
            task.putInt("progress", it.progress)
            task.putBool("claimed", it.claimed)
            val reward = SFSArray()
            it.reward.forEach {
                val item = SFSObject()
                item.putInt("item_id", it.itemId)
                item.putInt("quantity", it.quantity)
                if (it.expiration != 0L)
                    item.putLong("expiration", it.expiration)
                reward.addSFSObject(item)
            }
            task.putSFSArray("reward", reward)
            sfsArray.addSFSObject(task)
        }
        return sfsArray
    }

    fun toSfsObject(): SFSObject {
        val sfsObject = SFSObject()
        sfsObject.putUtfString("url_config", urlConfig)
        sfsObject.putBool("final_reward_claimed", finalRewardClaimed)
        sfsObject.putSFSArray("tasks", allTaskToSfsArray())
        return sfsObject
    }
}


data class UserTask(
    val taskId: Int,
    var progress: Int,
    var claimed: Boolean,
    var reward: List<ItemForClaim>
) {
    companion object {
        fun parseRewardForClaim(input: String): List<ItemForClaim> {
            return try {
                val rewards = input.trim('[', ']').split("],[")
                val rewardList = rewards.map { rewardString ->
                    rewardString.split(",").map { itemString ->
                        val parts = itemString.split(":")
                        val itemId = parts[0].toInt()
                        val quantity = parts[1].toInt()
                        val expiration = if (parts.size > 2) parts[2].toLong() else 0L
                        ItemForClaim(itemId, quantity, expiration)
                    }
                }.flatten()

                rewardList
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

}

data class ItemForClaim(
    val itemId: Int, val quantity: Int, val expiration: Long
)