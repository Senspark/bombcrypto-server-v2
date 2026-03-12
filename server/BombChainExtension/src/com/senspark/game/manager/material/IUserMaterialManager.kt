package com.senspark.game.manager.material

import com.senspark.game.data.model.user.UserMaterial
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject

interface IUserMaterialManager {
    fun getMaterials(): Map<Int, UserMaterial>
    fun loadMaterials()
    fun upgradeCrystal(itemId: Int, quantity: Int): ISFSObject
    fun toSfsArray(): ISFSArray
    fun checkEnoughCrystal(itemId: Int, quantity: Int)
} 