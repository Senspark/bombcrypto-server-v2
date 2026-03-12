package com.senspark.game.data

import com.smartfoxserver.v2.entities.data.ISFSObject

class EquipmentData(
    val equipmentId: Int,
    val equipmentType: Int,
    val heroId: Int,
    val network: Int,
    val userId: Int
) {
    constructor(obj: ISFSObject) : this(
        obj.getInt("equipment_id"),
        obj.getInt("equipment_id"),
        obj.getInt("hero_id"),
        obj.getInt("network"),
        obj.getInt("user_id")
    )
}