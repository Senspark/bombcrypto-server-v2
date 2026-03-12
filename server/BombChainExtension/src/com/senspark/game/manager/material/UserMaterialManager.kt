package com.senspark.game.manager.material

import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.manager.item.IConfigItemManager
import com.senspark.game.data.manager.upgradeHero.IUpgradeCrystalManager
import com.senspark.game.data.model.user.UserMaterial
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.EnumConstants.BLOCK_REWARD_TYPE
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.blockReward.IUserBlockRewardManager
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.util.concurrent.ConcurrentHashMap

const val UPGRADE_CRYSTAL_RATE = 4

class UserMaterialManager(
    private val _mediator: UserControllerMediator,
    private val userBlockRewardManager: IUserBlockRewardManager,
) : IUserMaterialManager {
    private val configItemManager = _mediator.svServices.get<IConfigItemManager>()
    private val upgradeCrystalManager = _mediator.svServices.get<IUpgradeCrystalManager>()
    private val userDataAccess = _mediator.services.get<IUserDataAccess>()
        
    private lateinit var _materials: ConcurrentHashMap<Int, UserMaterial>

    override fun getMaterials(): Map<Int, UserMaterial> {
        //if (!::_materials.isInitialized) {
        // Do có nhiều network có thể chơi cùng lúc nên khi lấy phải sync lại với database
            loadMaterials()
        //}
        return _materials
    }

    override fun toSfsArray(): ISFSArray {
        return getMaterials().values.toSFSArray { it.toSfsObject() }
    }

    override fun loadMaterials() {
        val m = userDataAccess.loadUserMaterial(_mediator.userId, configItemManager)
            .filter { it.value.quantity > 0 }
            .toSortedMap()
        _materials = ConcurrentHashMap(m)
    }

    override fun checkEnoughCrystal(itemId: Int, quantity: Int) {
        val havingQuantity = getMaterials()[itemId]?.quantity ?: 0
        if (quantity > havingQuantity) {
            throw CustomException("Not enough $quantity ${configItemManager.getItem(itemId).name}")
        }
    }

    override fun upgradeCrystal(itemId: Int, quantity: Int): ISFSObject {
        if (quantity % UPGRADE_CRYSTAL_RATE != 0) {
            throw CustomException("Quantity must be multiple of 4")
        }
        this.loadMaterials()
        val upgradeConfig = upgradeCrystalManager.getBySourceItemId(itemId)
        val goldFee = upgradeConfig.goldFee * quantity / UPGRADE_CRYSTAL_RATE
        val gemFee = upgradeConfig.gemFee * quantity / UPGRADE_CRYSTAL_RATE
        userBlockRewardManager.checkEnoughReward(goldFee.toFloat(), BLOCK_REWARD_TYPE.GOLD)
        userBlockRewardManager.checkEnoughReward(gemFee.toFloat(), BLOCK_REWARD_TYPE.GEM)
        checkEnoughCrystal(upgradeConfig.sourceItemId, quantity)
        //save
        userDataAccess.mergerUserCrystal(
            _mediator.userId,
            upgradeConfig.sourceItemId,
            upgradeConfig.targetItemId,
            quantity,
            UPGRADE_CRYSTAL_RATE,
            goldFee,
            gemFee
        )
        this.loadMaterials()
        this.userBlockRewardManager.loadUserBlockReward()
        return SFSObject().apply {
            putInt("item_id", upgradeConfig.targetItemId)
            putInt("quantity", quantity / UPGRADE_CRYSTAL_RATE)
        }
    }
}