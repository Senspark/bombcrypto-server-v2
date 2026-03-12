package com.senspark.game.manager.costumePreset

import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class NullUserCostumePresetManager : IUserCostumePresetManager {
    override fun toSfsObject(): ISFSObject {
        return SFSObject()
    }

    override fun createOrUpdate(id: String?, name: String, bomberId: Int, skinIds: List<Int>) {}
}
