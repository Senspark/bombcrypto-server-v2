package com.senspark.game.data.model.user

import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class UserClub(
    val clubId: Int,
    val name: String,
    var pointTotal: Double,
    var pointCurrentSeason: Double
) {
    fun toSFSObject(): ISFSObject {
        return SFSObject().apply {
            putUtfString("name", name)
            putDouble("point_total", pointTotal)
            putDouble("point_current_season", pointCurrentSeason)
        }
    }
}
