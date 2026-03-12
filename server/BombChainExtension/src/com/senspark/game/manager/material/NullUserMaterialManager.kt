package com.senspark.game.manager.material

import com.senspark.game.data.model.user.UserMaterial
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class NullUserMaterialManager : IUserMaterialManager {
    override fun getMaterials(): Map<Int, UserMaterial> {
        return emptyMap()
    }

    override fun toSfsArray(): ISFSArray {
        return SFSArray()
    }

    override fun loadMaterials() {}

    override fun checkEnoughCrystal(itemId: Int, quantity: Int) {}

    override fun upgradeCrystal(itemId: Int, quantity: Int): ISFSObject {
        return SFSObject()
    }
}