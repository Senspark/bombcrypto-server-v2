package com.senspark.game.data.model.config

import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.declare.customEnum.GachaChestType
import com.senspark.game.exception.CustomException
import com.senspark.game.utils.deserializeList
import com.senspark.game.utils.random.IWeightedRandom
import com.senspark.game.utils.random.WeightedRandomFloat
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet

interface IGachaChest {
    val type: GachaChestType
    val openTimeInMinute: Int
    val itemsQuantity: Int
    val skipOpenTimeGemRequire: Int
    val isSellable: Boolean
    val randomManager: IWeightedRandom<IGachaChestItem>
    fun toSFSObject(): ISFSObject
    fun getPrice(rewardType: BLOCK_REWARD_TYPE, quantity: Int): Int
    fun randomItems(): List<IGachaChestItem>
    fun allItems(): List<IGachaChestItem>
}

class GachaChest(
    override val type: GachaChestType,
    override val openTimeInMinute: Int,
    override val itemsQuantity: Int,
    private val goldPrice: Int?,
    private val gemPrice: Int?,
    private val coinPrice: Int?,
    override val skipOpenTimeGemRequire: Int,
    private val items: List<GachaChestItem>,
    override val isSellable: Boolean,
    private val isHasDiscount: Boolean,
) : IGachaChest {

    override val randomManager: IWeightedRandom<IGachaChestItem> = WeightedRandomFloat(items)

    @Transient
    private val priceArray: ISFSArray = SFSArray()

    init {
        val quantityPackages = listOf(1)
        mapOf(
            BLOCK_REWARD_TYPE.GEM to gemPrice,
            BLOCK_REWARD_TYPE.GOLD to goldPrice,
            BLOCK_REWARD_TYPE.COIN to coinPrice,
        ).forEach {
            it.value?.let { _ ->
                quantityPackages.forEach { quantity ->
                    priceArray.addSFSObject(SFSObject().apply {
                        putInt("reward_type", it.key.value)
                        putInt("quantity", quantity)
                        putInt("price", getPrice(it.key, quantity))
                    })
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun fromResultSet(rs: ResultSet, configItemManager: IConfigItemManager): GachaChest {
            return GachaChest(
                GachaChestType.fromValue(rs.getInt("type")),
                rs.getInt("open_time_in_minute"),
                rs.getInt("items_quantity"),
                if (rs.getObject("gold_price") == null) null else rs.getInt("gold_price"),
                if (rs.getObject("gem_price") == null) null else rs.getInt("gem_price"),
                if (rs.getObject("coin_price") == null) null else rs.getInt("coin_price"),
                rs.getInt("skip_open_time_gem_require"),
                deserializeList<GachaChestItem>(rs.getString("items")).map {
                    it.item = configItemManager.getItem(it.itemId)
                    it
                },
                rs.getBoolean("is_sellable"),
                rs.getBoolean("is_has_discount"),
            )
        }
    }

    override fun toSFSObject(): ISFSObject {
        return SFSObject.newInstance().apply {
            putInt("chest_type", type.value)
            putSFSArray("prices", priceArray)
            putInt("items_quantity", itemsQuantity)
            putIntArray("item_ids", items.map { it.itemId }.toSet())
        }
    }

    override fun randomItems(): List<IGachaChestItem> {
        return (1..itemsQuantity).map {
            GachaChestItemRandomized(randomManager.randomItem())
        }
    }

    override fun allItems(): List<IGachaChestItem> {
        return items.map { GachaChestItemRandomized(it) }
    }

    override fun getPrice(rewardType: BLOCK_REWARD_TYPE, quantity: Int): Int {
        val unitPrice = when (rewardType) {
            BLOCK_REWARD_TYPE.GOLD -> goldPrice
            BLOCK_REWARD_TYPE.GEM -> gemPrice
            BLOCK_REWARD_TYPE.COIN -> coinPrice
            else -> throw CustomException("Reward ${rewardType.name} not allow to buy gacha chest")
        } ?: throw CustomException("Reward not support to buy chest")
        return unitPrice * quantity
    }
}