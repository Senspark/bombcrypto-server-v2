package com.senspark.game.handler.adventure

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand.HERO_TAKE_DAMAGE_V2
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject

class HeroTakeDamageHandler : BaseEncryptRequestHandler() {
    override val serverCommand = HERO_TAKE_DAMAGE_V2

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val response = SFSObject()
        try {
            val timestamp: Long = data.getLong("timestamp")
            val heroId = data.getLong("hero_id").toInt()
            val enemyId = data.getInt("enemy_id")

            val masterManager = controller.masterUserManager
            val manager = masterManager.userAdventureModeManager
            val matchManager = manager.matchManager
            val hero = matchManager.hero
            val dame = if (heroId == enemyId) hero.dmg else matchManager.getEnemy(enemyId)?.damage ?: 0
            val storyImmortal = controller.userPermissions.storyImmortal
            matchManager.heroTakeDamage(if (storyImmortal) 0 else dame)
            val reviveHeroCost = manager.getReviveHeroCost()
            response.putLong("timestamp", timestamp)
            response.putLong("hero_id", heroId.toLong())
            response.putInt("hp", hero.hp)
            response.putBool("allow_revive", reviveHeroCost != null)
            if (reviveHeroCost != null) {
                response.apply {
                    putBool("allow_revive_by_ads", reviveHeroCost.getBool("allow_revive_by_ads"))
                    putInt("revive_gem_amount", reviveHeroCost.getInt("revive_gem_amount"))
                }
            }
            return sendSuccess(controller, requestId, response)
        } catch (ex: Exception) {
            return sendError(controller, requestId, 100, ex.message)
        }
    }
}