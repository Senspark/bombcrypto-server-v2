package com.senspark.game.handler.adventure

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand.ENEMY_TAKE_DAMAGE_V2
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class EnemyTakeDamageHandler: BaseEncryptRequestHandler() {
    override val serverCommand = ENEMY_TAKE_DAMAGE_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val response = SFSObject()
        val timestamp = data.getLong("timestamp")
        response.putLong("timestamp", timestamp)
        val matchManager = controller.masterUserManager.userAdventureModeManager.matchManager
        try {
            val enemyId = data.getInt("enemy_id")
            val pair = matchManager.enemyTakeDamage(enemyId)
            val enemyHp = matchManager.getEnemy(enemyId)?.health ?: -100f
            response.putInt("id", enemyId)
            response.putFloat("hp", enemyHp)
            response.putInt("damage", pair.first)
            response.putInt("gold_receive", pair.second)
            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            return sendError(controller, requestId, 100, ex.message)
        }
    }
}