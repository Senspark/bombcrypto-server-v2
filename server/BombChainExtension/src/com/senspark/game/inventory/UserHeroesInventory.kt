package com.senspark.game.inventory

import com.senspark.game.constant.ItemType
import com.senspark.game.data.model.Filter
import com.senspark.game.data.model.user.UserItem
import com.senspark.game.manager.hero.IUserHeroTRManager
import java.time.Instant

class UserHeroesInventory(private val _userHeroTRManager: IUserHeroTRManager) : IUserInventory {

    override fun get(
        uid: Int,
        filter: Filter
    ): Map<Int, List<UserItem>> {
        val result = mutableMapOf<Int, MutableList<UserItem>>()
        _userHeroTRManager.heroesSoulMapByItemId.forEach {
            it.value.heroes.forEach { heroMapByStatus ->
                val userItem = UserItem(
                    id = heroMapByStatus.value[0].heroId,
                    type = ItemType.HERO,
                    itemId = it.key,
                    status = heroMapByStatus.key.value,
                    equipStatus = heroMapByStatus.key.value,
                    Instant.now().toEpochMilli(),
                    null,
                    quantity = heroMapByStatus.value.size,
                    expirationAfter = null,
                    itemInstantIds = heroMapByStatus.value.map { hero -> hero.heroId }
                )
                val items = result[userItem.itemId] ?: mutableListOf()
                items.add(userItem)
                result[userItem.itemId] = items
            }
        }
        return result
    }

    override fun insert() {
        TODO("Not yet implemented")
    }

    override fun update() {
        TODO("Not yet implemented")
    }
}