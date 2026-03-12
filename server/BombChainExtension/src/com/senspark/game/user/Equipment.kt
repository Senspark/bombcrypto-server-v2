package com.senspark.game.user

import com.senspark.game.data.EquipmentData
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class Equipment(data: EquipmentData) {
    val equipmentId = data.equipmentId
    val equipmentType = data.equipmentType
    val heroId = data.heroId

    fun toSFSObject(): ISFSObject {
        val result = SFSObject()
        result.putInt("equipment_id", equipmentId)
        result.putInt("equipment_type", equipmentType)
        result.putInt("hero_id", heroId)
        return result
    }
}