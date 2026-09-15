package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.*
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.blockMap.HeroTargetRequest
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

// Called when client has no target for some heroes (see server/docs/explode_v6.md).
// Heroes with no legal target are omitted from the response.
class GetBombTargetHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_BOMB_TARGET

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        scheduler.fireAndForget {
            try {
                doWork(controller, requestId, data)
            } catch (e: CustomException) {
                sendExceptionError(controller, requestId, e)
            }
        }
    }

    private fun doWork(controller: IUserController, requestId: Int, data: ISFSObject) {
        val heroesArr = data.getSFSArray("heroes")
        val bbmController = controller.masterUserManager.heroFiManager
        val blockMap = controller.masterUserManager.userBlockMapManagerV2

        val validHeroes = mutableMapOf<Int, Hero>()
        val requests = mutableListOf<HeroTargetRequest>()

        synchronized(blockMap.locker) {
            for (idx in 0 until (heroesArr?.size() ?: 0)) {
                val entry = heroesArr.getSFSObject(idx)
                val heroId = entry.getInt("id")
                val bbm = bbmController.getHero(heroId, controller.dataType) ?: continue
                if (bbm.details.dataType != controller.dataType) continue
                if (!bbm.isActive) continue
                if (bbm.stage != GameConstants.BOMBER_STAGE.WORK) continue

                val seed: Pair<Int, Int>? = if (entry.containsKey("i") && entry.containsKey("j")) {
                    entry.getInt("i") to entry.getInt("j")
                } else {
                    null
                }
                val reject: Pair<Int, Int>? = if (entry.containsKey("reject_i") && entry.containsKey("reject_j")) {
                    entry.getInt("reject_i") to entry.getInt("reject_j")
                } else {
                    null
                }

                validHeroes[heroId] = bbm
                requests.add(HeroTargetRequest(heroId, seed, reject))
            }

            val results = blockMap.getOrCreateTargets(requests)

            val targets: ISFSArray = SFSArray()
            for (result in results) {
                val bbm = validHeroes[result.heroId] ?: continue
                val targetObj: ISFSObject = SFSObject()
                targetObj.putLong(SFSField.ID, bbm.heroId.toLong())
                targetObj.putInt(SFSField.HeroType, bbm.type.value)
                targetObj.putInt("i", result.target.first)
                targetObj.putInt("j", result.target.second)
                targets.addSFSObject(targetObj)
            }

            val resultData: ISFSObject = SFSObject()
            resultData.putSFSArray(SFSField.Targets, targets)
            sendSuccess(controller, requestId, resultData)
        }
    }
}
