package com.senspark.game.manager

import com.senspark.common.utils.IServerLogger
import com.senspark.common.utils.toSFSArray
import com.senspark.game.api.*
import com.senspark.game.constant.ItemStatus
import com.senspark.game.constant.ItemType
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.model.Filter
import com.senspark.game.data.model.config.MarketItemConfig
import com.senspark.game.data.model.config.MarketplaceItemGrouped
import com.senspark.game.data.model.user.UserItem
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.senspark.game.manager.hero.IUserHeroTRManager
import com.senspark.game.manager.market.IMarketManager
import com.senspark.game.manager.material.IUserMaterialManager
import com.senspark.game.manager.user.IUserOldItemManager
import com.senspark.game.utils.serialize
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class UserMarketplaceManager(
    private val _mediator: UserControllerMediator,
    private val _userHeroTrManager: IUserHeroTRManager,
    private val _userOldItemManager: IUserOldItemManager,
    private val _userMaterialManager: IUserMaterialManager,
    private val _blockRewardManager: IUserBlockRewardManager,
) : IUserMarketplaceManager {

    private val _configItemManager = _mediator.svServices.get<IConfigItemManager>()
    private val _logger = _mediator.svServices.get<IServerLogger>()
    private val _marketApi = _mediator.svServices.get<IMarketApi>()
    private val _configManager: IGameConfigManager = _mediator.services.get<IGameConfigManager>()
    private val _userDataAccess = _mediator.services.get<IUserDataAccess>()
    private val _marketManager = _mediator.svServices.get<IMarketManager>()


    private val _feeSell = getFeeSell()
    private val _configMarket : Map<Number, MarketItemConfig> = _marketManager.getMarketConfig().associateBy { it.itemId }

    override fun order(orderData: OrderDataRequest): ISFSObject {
        // Basic check price, user must have minimum gem = min_price * quantity
        val minimumGem = getMinPrice(orderData.itemId) * orderData.quantity
        
        // Gửi số gem của user này cho api check xem có đc order ko
        val currentGem  = _blockRewardManager.getRewardValue(BLOCK_REWARD_TYPE.GEM) +
                _blockRewardManager.getRewardValue(BLOCK_REWARD_TYPE.GEM_LOCKED)
        
        if (currentGem < minimumGem) {
            throw CustomException("Not enough gem")
        }
        
        orderData.userGem = currentGem
        
        val orderResponse = _marketApi.order(orderData)
        return SFSObject().apply {
            putBool("success", orderResponse.isOrderSuccess)
            putInt("total_quantity", orderResponse.totalQuantity)
            putInt("total_price", orderResponse.totalPrice)
        }
    }

    override fun cancelOrder(buyerUid: Int): Boolean {
        return _marketApi.cancelOrder(buyerUid)
    }

    override fun buyV3(buyerUid: Int): BuyDataResponse {
        if (_mediator.isCheatByMultipleLogin()) {
            throw Exception("Hash invalid")
        }

        return _marketApi.buy(buyerUid)
    }

    override fun sellV3(sellData: SellOrEditDataRequest): Boolean {
        checkUserPermission()
        //Do có nhiều client cùng gọi cùng lúc nên phải load lại từ db mỗi khi gọi market
        _userHeroTrManager.loadHero(true)

        //check price
        if (sellData.price < 1) {
            throw CustomException("Selling price must be greater than 1")
        }
        if (sellData.quantity < 1) {
            throw CustomException("Selling quantity must be greater than 1")
        }
        val items =
            (getUserInventory(
                _mediator.userId,
                Filter(itemId = sellData.itemId, expirationAfter = sellData.expiration * 1000)
            )[sellData.itemId]
                ?: emptyList())
                .filter { it.status == ItemStatus.Normal.value }
        val itemIds = items.map { it.itemInstantIds }.flatten()
        if (itemIds.size < sellData.quantity) {
            throw CustomException("Not enough item to sell")
        }
        val listIdUserSell = itemIds.take(sellData.quantity)
        sellData.listId = listIdUserSell
        val result = _marketApi.sell(sellData)
        if(sellData.itemType == ItemType.HERO.value) {
            _userHeroTrManager.loadHero(true)
        }
        return result
    }

    override fun editV3(editData: SellOrEditDataRequest): Boolean {
        checkUserPermission()
        //Do có nhiều client cùng gọi cùng lúc nên phải load lại từ db mỗi khi gọi market
        _userHeroTrManager.loadHero(true)
        //check price
        if (editData.price < 1) {
            throw CustomException("Selling price must be greater than 1")
        }
        if (editData.quantity < 1) {
            throw CustomException("Selling quantity must be greater than 1")
        }
//        Để edit số lượng nhiều hơn nhưng hiện design chỉ cho edit <= số lượng cũ nên tạm comment
//        val listIdUserSell: List<Int> = emptyList()
//        val expiration  = if(editData.expiration == 0) {
//            -1
//        } else {
//            editData.expiration * 1000
//        }
//        val itemHaveLeft =
//            (getUserInventory(
//                _mediator.userId,
//                Filter(itemId = editData.itemId, expirationAfter = expiration)
//            )[editData.itemId]
//                ?: emptyList())
//                .filter { it.status == ItemStatus.Normal.value }
        
//        val itemHaveLeftId = itemHaveLeft.map { it.itemInstantIds }.flatten()
        
        // Bán thêm nhiều hơn số hero đang có
        val itemNeedMore = editData.quantity - editData.oldQuantity
        
        if(itemNeedMore > 0){
            throw CustomException("Not enough item")
        }
        // Ko cho edit thêm số lượng
//        if(itemNeedMore > itemHaveLeftId.size) {
//            throw CustomException("Incorrect quantity")
//        }
//        if(itemNeedMore < 0) {
//            itemNeedMore = editData.quantity
//        }
//        listIdUserSell = itemHaveLeftId.take(itemNeedMore)
//        editData.listId = listIdUserSell
        editData.listId = emptyList()
        val result = _marketApi.edit(editData)
        if (editData.itemType == ItemType.HERO.value) {
            _userHeroTrManager.loadHero(true)
        }
        return result
    }

    override fun cancelV3(cancelData: CancelDataRequest): Boolean {
        checkUserPermission()
        //Do có nhiều client cùng gọi cùng lúc nên phải load lại từ db mỗi khi gọi market
        _userHeroTrManager.loadHero(true)

        val result = _marketApi.cancel(cancelData)
        if(cancelData.itemType == ItemType.HERO.value) {
            _userHeroTrManager.loadHero(true)
        }
        return result
    }

    override fun getMyItemMarket(): ISFSObject {
        val myItemMarketList = _marketApi.getMyItem(_mediator.userId)
        val myItemMarket = MarketplaceItemGrouped.fromMyItemList(myItemMarketList, _configItemManager)
        return SFSObject().apply {
            putSFSArray("data", myItemMarket.toSFSArray { it.toSfsObject() })
        }
    }

    override fun getMarketConfig(): ISFSObject {
        val config = _marketManager.getMarketConfig()
        return SFSObject().apply {
            putSFSArray("data", config.toSFSArray { it.toSfsObject() })
            putInt("refresh_min_price_client", _configManager.refreshMinPriceClient)
        }
    }

    override fun getCurrentMinPrice(): ISFSObject {
        return _marketManager.getCurrentMinPrice()
    }

    override fun getUserInventory(filter: Filter): UserItem {
        val userInventory = getUserInventory(_mediator.userId, filter).values.flatten()
        return userInventory.first { it.id == filter.id }
    }

    override fun getUserInventoryToSFSArray(filter: Filter): ISFSObject {
        val results = SFSObject()
        results.putFloat("fee_sell", _feeSell)
        //get items no locked
        val userInventory = getUserInventory(_mediator.userId, filter).values.flatten()
            .filter { it.status != ItemStatus.Sell.value }
            .groupBy { item -> Triple(item.itemId, item.status, item.expirationAfter) }.mapValues { item ->
                item.value.reduce { item, _ ->
                    item.quantity += 1
                    item
                }
            }
        results.putSFSArray(
            "data",
            userInventory.values.sortedBy { it.itemId }.toSFSArray {
                SFSObject.newFromJsonData(it.serialize()).apply {
                    putBool("is_new", _userOldItemManager.isNewItem(it.itemId))
                }
            })
        return results
    }

    override fun getActivity(): ISFSArray {
        val activity = _userDataAccess.getActivity(_mediator.userId)
        val results = SFSArray()
        activity.values.sortedByDescending { it.date }.forEach { results.addSFSObject(it.toSfsObject()) }
        return results
    }

    private fun checkUserPermission() {
        if (_mediator.userType.isUserTraditional) {
            throw CustomException(
                "User traditional can not cancel item"
            )
        }
        if (_mediator.isCheatByMultipleLogin()) {
            throw CustomException("Hash invalid")
        }
    }

    private fun getUserInventory(uid: Int, filter: Filter): Map<Int, List<UserItem>> {
        return ItemType.fromValue(
            if (filter.itemId != -1)
                _configItemManager.getItem(filter.itemId).type.value else filter.type
        )
            .newInstance(_userHeroTrManager, _userMaterialManager, _userDataAccess)
            .get(uid, filter)
    }

    private fun getFeeSell(): Float {
        return _configManager.feeSell
    }
    private fun getMinPrice(itemId: Int): Double {
        return _configMarket[itemId]?.minPrice ?: 1.0
    }
}
