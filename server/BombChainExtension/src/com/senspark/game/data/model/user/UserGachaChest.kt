package com.senspark.game.data.model.user

import com.senspark.game.data.model.config.IGachaChest
import com.senspark.game.declare.customEnum.GachaChestType
import com.senspark.game.user.IGachaChestManager
import java.sql.ResultSet
import java.time.Instant

class UserGachaChest(
    val id: Int,
    val chestConfig: IGachaChest,
    val chestType: GachaChestType,
    var openTime: Long,
) {

    val remainingOpenTime: Long
        get() {
            return 0
//            if (openTime == -1L)
//                return -1
//            val remainingTime = chestConfig.openTimeInMinute * 60 * 1000 - (Instant.now().toEpochMilli() - openTime)
//            if (remainingTime <= 0)
//                return 0
//            return remainingTime
        }

    companion object {
        fun fromResulSet(sfsObject: ResultSet, gachaChestManager: IGachaChestManager): UserGachaChest {
            val type = GachaChestType.fromValue(sfsObject.getInt("chest_type"))
            return UserGachaChest(
                sfsObject.getInt("chest_id"),
                gachaChestManager.getChest(type),
                type,
                sfsObject.getLong("open_time")
            )
        }
    }
}