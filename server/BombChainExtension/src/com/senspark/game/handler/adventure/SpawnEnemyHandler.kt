package com.senspark.game.handler.adventure

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand.SPAWN_ENEMY_V2
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class SpawnEnemyHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SPAWN_ENEMY_V2

    private val _skins = mapOf(
        8 to 7,
        12 to 11,
        16 to 13,
        20 to 18,
        24 to 22,
        28 to 27,
        32 to 33
    )
    private val _nums = mapOf(
        8 to 2,
        12 to 1,
        16 to 1,
        20 to 2,
        24 to 2,
        28 to 3,
        32 to 3
    )

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val storyMatchManager = controller.masterUserManager.userAdventureModeManager.matchManager
        val skin: Int = data.getInt("skin")
        val response = SFSObject()
        response.putLong("timestamp", data.getLong("timestamp"))
        try {
            val enemies = SFSArray()
            storyMatchManager.spawnEnemies(
                _skins[skin] ?: throw Exception("Could find enemy id $skin"),
                _nums[skin] ?: throw Exception("Could find enemy id $skin")
            ).forEach { enemies.addSFSObject(it.toObject()) }
            response.putSFSArray(
                "enemies",
                enemies
            )
            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            return sendError(controller, requestId, 100, ex.message)
        }
    }
}