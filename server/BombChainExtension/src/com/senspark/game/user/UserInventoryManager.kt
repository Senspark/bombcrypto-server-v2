package com.senspark.game.user

import com.senspark.common.constant.ExpirationAfter
import com.senspark.common.constant.ItemId
import com.senspark.common.constant.SkinId
import com.senspark.game.constant.ItemPackage
import com.senspark.game.constant.ItemType
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.OpenSkinChestData
import com.senspark.game.data.manager.hero.IConfigHeroTraditionalManager
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.model.config.Item
import com.senspark.game.data.model.user.AddUserItemWrapper
import com.senspark.game.data.model.user.UserItem
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IRewardDataAccess
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.senspark.game.manager.hero.IUserHeroTRManager
import com.senspark.game.manager.material.IUserMaterialManager
import com.senspark.game.service.IPvpDataAccess
import com.senspark.game.utils.SkinChestRandom
import com.senspark.lib.data.manager.GameConfigManager
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import java.time.Instant

class UserInventoryManager(
    private val _mediator: UserControllerMediator,
    private val _userBlockRewardManager: IUserBlockRewardManager,
    private val _userHeroManager: IUserHeroTRManager,
    private val _userMaterial: IUserMaterialManager,
) : IUserInventoryManager {

    private val _dataAccessManager = _mediator.services.get<IDataAccessManager>()
    private val _gameConfigManager = _mediator.services.get<IGameConfigManager>()
    private val _database: IPvpDataAccess = _dataAccessManager.pvpDataAccess
    private val _rewardDataAccess: IRewardDataAccess = _dataAccessManager.rewardDataAccess
    private val _userDataAccess: IUserDataAccess = _dataAccessManager.userDataAccess
    
    private val _configHeroTraditionalManager = _mediator.svServices.get<IConfigHeroTraditionalManager>()
    private val _configItemManager = _mediator.svServices.get<IConfigItemManager>()
    
    private val _random: SkinChestRandom = SkinChestRandom(_dataAccessManager.pvpDataAccess.querySkinChestDropRate())
    private val _openChestCost: Float = _gameConfigManager.openSkinChestCost
    
    private lateinit var _itemsMapById: MutableMap<SkinId, SkinChest>
    private lateinit var _itemMapByItemId: MutableMap<ItemId, List<SkinChest>>
    private lateinit var _activeItems: MutableMap<ItemType, List<SkinChest>>
    private var _userEquip: MutableMap<ItemType, Int> = mutableMapOf()
    private val locker = Any()

    init {
        loadInventory()
    }

    override fun loadInventory() {
        setData(
            getUserInventory()
        )
    }

    override fun isHavingSkin(skinIds: List<Int>): Boolean {
        return skinIds.all { _itemsMapById.containsKey(it) }
    }

    override fun isHavingSkin(skinId: Int): Boolean {
        return _itemsMapById.containsKey(skinId)
    }

    private fun getUserInventory(): Map<ItemId, List<UserItem>> {
        return _userDataAccess.getUserInventory(_mediator.userId)
    }

    override val activeSkinChests: Map<ItemType, List<ItemId>>
        get() = _activeItems.mapValues { it.value.map { it2 -> it2.itemId } }

    override fun activeSkinChest(itemId: Int, expirationAfter: Long?) {
        val sameItems = _itemMapByItemId[itemId]?.filter {
            expirationAfter == null || it.expirationAfter == null || it.expirationAfter == expirationAfter
        } ?: emptyList()
        if (sameItems.isEmpty())
            throw Exception("Invalid item id: $itemId")

        val item = sameItems.firstOrNull {
            val expiryDate = it.expiryDate
            return@firstOrNull (expiryDate != null && expiryDate > Instant.now())
        } ?: sameItems.firstOrNull { it.expiryDate == null }
        ?: throw Exception("No valid item can active, item id: $itemId")

        var status = 1
        if (item.active) {
            status = 0
        }
        _database.updateSkinChest(item.id, _mediator.userId, status)
        setData(getUserInventory())
    }

    override fun activeSkinChest(itemType: ItemType, items: Map<ItemId, ExpirationAfter?>) {
        val activeSkinIds: List<Int>
        // case empty -> de active skin chest
        if (items.isEmpty()) {
            activeSkinIds = emptyList()
        } else {
            require(itemType.maxActive >= items.size) { "Max Active ${itemType.maxActive}" }
            val items = items.mapValues {
                _itemMapByItemId[it.key]?.filter { it2 ->
                    it2.type == itemType && (it.value == null || it2.expirationAfter == null || it2.expirationAfter == it.value)
                } ?: emptyList()
            }
            activeSkinIds = items.map {
                it.value.firstOrNull { it2 ->
                    val expiryDate = it2.expiryDate
                    return@firstOrNull (expiryDate != null && expiryDate > Instant.now())
                } ?: it.value.firstOrNull { it2 -> it2.expiryDate == null }
                ?: throw Exception("No valid item can active, item id: $items")
            }.map { it.id }
        }
        _database.updateSkinChest(_mediator.userId, itemType, activeSkinIds)
        setData(getUserInventory())
    }

    override fun openSkinChest(): SkinChest {
        val itemId = _random.random()
        val item = OpenSkinChestData(itemId, _configItemManager.getItem(itemId).type)
        _database.updateSkinChest(_openChestCost, item, _mediator.userId, _mediator.userName)
        val items = getUserInventory()
        val newItem = items.values.flatten().first { !_itemsMapById.containsKey(it.id) }
        setData(items)
        return SkinChest(newItem.id, item.itemId, item.type, 0, null, null)
    }

    private fun setData(data: Map<Int, List<UserItem>>) {
        synchronized(locker) {
            //check has default items
            checkAndInsertDefaultItem(data)
            //disable skin out of date
            val items = data.values.flatten()
                .map { SkinChest(it) }
                .filter { it.expiryDate == null || it.expiryDate!! >= Instant.now() }
            _itemsMapById = items.associateBy { it.id }.toMutableMap()
            _itemMapByItemId = items.groupBy { it.itemId }.toMutableMap()
            _activeItems = items.filter { it.active }.groupBy { it.type }.toMutableMap()
        }
    }

    private fun checkAndInsertDefaultItem(havingItems: Map<ItemId, List<UserItem>>) {
        val missingItems = mutableSetOf<Item>()
        _configItemManager.itemsDefault.forEach {
            if (!havingItems.containsKey(it.id)) {
                missingItems.add(it)
            }
        }
        val sortedMissingItems = missingItems.sortedBy { it.id }
        if (sortedMissingItems.isNotEmpty()) {
            val addItems = sortedMissingItems.map {
                val canEquip = canEquipItem(it)
                AddUserItemWrapper(
                    it,
                    1,
                    true,
                    userId = _mediator.userId,
                    configHeroTraditional = _configHeroTraditionalManager,
                    isEquip = canEquip
                )
            }
            _rewardDataAccess.addTRRewardForUser(_mediator.userId, _mediator.dataType, addItems, {}, "Default_item")
            setData(getUserInventory())
        }
        _userEquip.clear()
    }

    private fun canEquipItem(item: Item): Boolean {
        val itemType = ItemType.fromValue(item.type.value)
        val maxActive = itemType.maxActive
        
        if (!_userEquip.containsKey(itemType)) {
            _userEquip[itemType] = 1
        }
        else {
            _userEquip[itemType] = _userEquip[itemType]!! + 1
        }
        
        return _userEquip[itemType]!! <= maxActive
    }

    override fun getInventoryToSFSArray(): ISFSArray {
        setData(getUserInventory())
        return SFSArray().apply {
            _itemMapByItemId.forEach { it ->
                val skinItems = it.value.groupBy { it.expirationAfter }
                skinItems.forEach { entry ->
                    val itemHasExpiryDate = entry.value.sortedBy { it.expiryDate }.firstOrNull { it.expiryDate != null }
                    val active = entry.value.any { it.active }
                    addSFSObject(SFSObject().apply {
                        putIntArray("ids", it.value.map { it.id })
                        putInt("item_id", it.key)
                        putInt("type", entry.value.first().type.value)
                        putInt("quantity", entry.value.size)
                        putBool("active", active)
                        itemHasExpiryDate?.let {
                            putLong("expiry_date", itemHasExpiryDate.expiryDate!!.toEpochMilli())
                        }
                        putLong("expiration_after", entry.key ?: 0)
                    })
                }
            }
        }
    }

    override fun buyCostumeItem(itemId: Int, itemPackage: ItemPackage, quantity: Int) {
        val allowBuyItemTypes = setOf(ItemType.BOMB, ItemType.TRAIL, ItemType.FIRE, ItemType.WING, ItemType.HERO, ItemType.EMOJI, ItemType.AVATAR)
        val item = _configItemManager.getItem(itemId)
        if (!allowBuyItemTypes.contains(item.type)) {
            throw CustomException("Item not allow to buy")
        }
        val unitPrice = item.getPrice(itemPackage) ?: throw CustomException("Package not available")
        val price = unitPrice * quantity
        val rewardHaving = when (itemPackage.rewardType) {
            EnumConstants.BLOCK_REWARD_TYPE.GEM -> {
                _userBlockRewardManager.getTotalGemHaving()
            }

            EnumConstants.BLOCK_REWARD_TYPE.GOLD -> {
                _userBlockRewardManager.getTotalGoldHaving()
            }

            else -> throw CustomException("Not support reward type")
        }
        if (rewardHaving < price) throw CustomException("Not enough $price ${itemPackage.rewardType}")
        buyCostumeItem(item, itemPackage, price.toFloat(), quantity)
    }

     override fun addItem(itemId: Int, quantity: Int, expiration: Long, reason: String?) {
        val allowBuyItemTypes = setOf(ItemType.BOOSTER, ItemType.REWARD, ItemType.HERO, ItemType.FIRE, ItemType.BOMB, ItemType.TRAIL, ItemType.WING, ItemType.MATERIAL)
        val item = _configItemManager.getItem(itemId)
        if (!allowBuyItemTypes.contains(item.type)) {
            throw CustomException("Item not allow to claim " + item.type)
        }
         val rewardsReceive = mutableListOf<AddUserItemWrapper>().apply {
             add(
                 AddUserItemWrapper(
                     item,
                     quantity,
                     true,
                     expiration,
                     userId = _mediator.userId,
                     configHeroTraditional = _configHeroTraditionalManager
                 )
             )
         }
         _rewardDataAccess.addTRRewardForUser(
             _mediator.userId,
             _mediator.dataType,
             rewardsReceive,
             {
                 _userHeroManager.loadHero(true)
                 _userMaterial.loadMaterials()
             },
             reason ?: "Add_item"
         )
    }
    
    private fun buyCostumeItem(item: Item, itemPackage: ItemPackage, price: Float, quantity: Int) {
        val rewardsReceive = mutableListOf<AddUserItemWrapper>().apply {
            add(
                AddUserItemWrapper(
                    item,
                    quantity,
                    true,
                    itemPackage.expirationAfter ?: 0,
                    userId = _mediator.userId,
                    configHeroTraditional = _configHeroTraditionalManager
                )
            )
        }
        _rewardDataAccess.addTRRewardForUser(
            _mediator.userId,
            _mediator.dataType,
            rewardsReceive,
            {
                _userBlockRewardManager.loadUserBlockReward()
                _userHeroManager.loadHero(true)
            },
            "Buy_custom_item",
            mapOf(itemPackage.rewardType to price)
        )
    }

    override fun destroy() = Unit
}