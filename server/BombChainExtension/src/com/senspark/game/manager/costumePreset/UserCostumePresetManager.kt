package com.senspark.game.manager.costumePreset

import com.senspark.common.utils.toSFSArray
import com.senspark.game.controller.UserControllerMediator
import com.senspark.game.data.model.user.IUserCostumePreset
import com.senspark.game.data.model.user.UserCostumePreset
import com.senspark.game.db.IDataAccessManager
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.exception.CustomException
import com.senspark.game.manager.config.IUserConfigManager
import com.senspark.game.manager.hero.IUserHeroTRManager
import com.senspark.game.user.IUserInventoryManager
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.util.*

interface IUserCostumePresetManager {
    fun toSfsObject(): ISFSObject
    fun createOrUpdate(id: String?, name: String, bomberId: Int, skinIds: List<Int>)
}

class UserCostumePresetManager(
    private val _mediator: UserControllerMediator,
    private val userSkinInventoryManager: IUserInventoryManager,
    private val userHeroTRManager: IUserHeroTRManager,
    private val userConfigManager: IUserConfigManager,
) : IUserCostumePresetManager {

    private val dataAccessManager = _mediator.services.get<IDataAccessManager>()
    private val userDataAccess: IUserDataAccess = dataAccessManager.userDataAccess
    
    private lateinit var costumePresets: List<IUserCostumePreset>

    init {
        loadPreset()
    }

    private fun loadPreset() {
        costumePresets = userDataAccess.loadUserCostumerPreset(_mediator.userId)
    }

    override fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putInt("max_slot", userConfigManager.totalCostumePresetSlot)
            putSFSArray("costume_presets", costumePresets.toSFSArray {
                it.toSfsObject(
                    { heroId -> userHeroTRManager.isHavingHero(heroId) },
                    { skinId -> userSkinInventoryManager.isHavingSkin(skinId) }
                )
            })
        }
    }


    override fun createOrUpdate(id: String?, name: String, bomberId: Int, skinIds: List<Int>) {
        require(userSkinInventoryManager.isHavingSkin(skinIds)) {
            "Skin invalid"
        }
        require(userHeroTRManager.isHavingHero(bomberId)) {
            "Hero invalid"
        }
        val sameNameCount = costumePresets.count { it.idSameNameWithOther(other = name) }
        val item = if (id != null) {
            costumePresets.firstOrNull { it.id == id }?.apply {
                this.name = parseName(name, sameNameCount)
                this.originalName = name
                this.bomberId = bomberId
                this.skinIds = skinIds
            } ?: throw CustomException("Preset invalid")
        } else {
            require(userConfigManager.totalCostumePresetSlot > costumePresets.size) {
                "Maximum slot"
            }
            UserCostumePreset(
                id = UUID.randomUUID().toString(),
                uid = _mediator.userId,
                name = parseName(name, sameNameCount),
                originalName = name,
                bomberId = bomberId,
                skinIds = skinIds
            )
        }
        userDataAccess.saveUserCostumerPreset(_mediator.userId, item)
        loadPreset()
    }


    private fun parseName(name: String, sameNameCount: Int): String {
        return if (sameNameCount == 0) name else "$name ($sameNameCount)"
    }

}
