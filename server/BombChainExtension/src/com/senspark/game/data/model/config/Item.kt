package com.senspark.game.data.model.config

import com.senspark.game.constant.*
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant


class Item(
    val id: Int,
    val type: ItemType,
    val name: String,
    abilities: Set<ItemAbility>,
    val kind: ItemKind,
    private val descriptionEn: String?,
    private val goldPrice7Days: Int?,
    private val gemPrice7Days: Int?,
    private val gemPrice30Days: Int?,
    private val gemPrice: Int?,
    private val goldPrice: Int?,
    val isSellable: Boolean,
    tag: String?,
    /**item default cho user, khi vào game sẽ tự động được nhận**/
    val isDefault: Boolean,
    private val saleStartDate: Long?,
    private val saleEndDate: Long?,
) {
    private val abilityJson = Json.encodeToString(abilities.map { it.name })
    private val pricesArray: ISFSArray = SFSArray()
    val speedBonus = if (abilities.contains(ItemAbility.SPEED_PLUS_1)) 1 else 0
    val dmgBonus = if (abilities.contains(ItemAbility.DMG_PLUS_1)) 1 else 0
    val rangeBonus = if (abilities.contains(ItemAbility.RANGE_PLUS_1)) 1 else 0
    val bombBonus = if (abilities.contains(ItemAbility.BOMB_PLUS_1)) 1 else 0
    val hpBonus = if (abilities.contains(ItemAbility.HP_PLUS_1)) 1 else 0
    private val tag: ItemTag? = if (tag == null) null else ItemTag.valueOf(tag)
    val sortIndex = when (this.tag) {
        ItemTag.LIMITED -> {
            1
        }

        ItemTag.NEW -> {
            2
        }

        else -> {
            3
        }
    }

    val isSaleOnShop: Boolean
        get() {
            if (!isSellable) {
                // Item is not marked as sellable, cannot be sold
                return false
            }
            val currentTime = Instant.now().epochSecond
            if (saleStartDate != null && currentTime < saleStartDate) {
                // Sale has not started yet, cannot be sold
                return false
            }
            // Sale has ended, cannot be sold
            return !(saleEndDate != null && currentTime >= saleEndDate)
            // All conditions have passed, item can be sold
        }

    init {
        ItemPackage.values().forEach {
            val price = getPrice(it)
            if (price != null) {
                pricesArray.addSFSObject(
                    SFSObject().apply {
                        putUtfString("package", it.name)
                        putInt("price", price)
                        it.expirationAfter?.let { it2 ->
                            putLong("duration", it2)
                        }
                        putUtfString("reward_type", it.rewardType.name)
                    }
                )
            }
        }
    }

    fun getPrice(itemPackage: ItemPackage): Int? {
        return when (itemPackage) {
            ItemPackage.GOLD_7 -> goldPrice7Days
            ItemPackage.GEM_7 -> gemPrice7Days
            ItemPackage.GEM_30 -> gemPrice30Days
            ItemPackage.BUY_GOLD -> goldPrice
            ItemPackage.BUY_GEM -> gemPrice
        }
    }

    fun toSFSObject(): ISFSObject {
        val result = SFSObject().apply {
            putInt("item_id", id)
            putInt("type", type.value)
            putUtfString("name", name)
            putUtfString("abilities", abilityJson)
            putUtfString("kind", kind.name)
            if (descriptionEn != null) putUtfString(
                "description_en",
                descriptionEn
            ) else putNull("description_en")
            putSFSArray("prices", pricesArray)
            tag?.let {
                putInt("tag", tag.value)
            }
        }

        return result
    }

    fun toSFSObjectForCostumeShop(): ISFSObject {
        return SFSObject().apply {
            putInt("item_id", id)
            putSFSArray("prices", pricesArray)
        }
    }
}