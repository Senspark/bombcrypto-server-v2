package com.senspark.game.handler.shield

import com.senspark.game.controller.IUserController
import com.senspark.game.controller.LegacyUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

/**
 * Resets the shield of multiple heroes in a single request.
 * Same logic as [RepairShieldHandler], but iterates over a list of hero ids.
 * Failures are isolated per hero (partial success): heroes that fail go to "failed"
 * and don't interrupt the others.
 */
class RepairShieldBatchHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.REPAIR_SHIELD_BATCH

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        val user = controller.user
            ?: return sendError(controller, requestId, ErrorCode.SERVER_ERROR, "User not found")

        controller as LegacyUserController
        if (controller.disableWhileLoginByAccount()) {
            return sendError(controller, requestId, ErrorCode.PERMISSION_DENIED, null)
        }

        val currentTime = System.currentTimeMillis()
        val lastRepairTime = user.getProperty("last_repair_time") as? Long ?: 0L
        val cooldownMs = 2000L

        if (currentTime - lastRepairTime < cooldownMs) {
            return sendError(controller, requestId, ErrorCode.SERVER_ERROR, "Request too fast. Please wait.")
        }

        return try {
            val heroList = data.getIntArray("hero_list").toList()
            if (heroList.isEmpty()) {
                return sendError(controller, requestId, ErrorCode.INVALID_PARAMETER, "Empty hero list")
            }
            // Only consume the cooldown after the request is valid
            user.setProperty("last_repair_time", currentTime)
            val rewardType = EnumConstants.BLOCK_REWARD_TYPE.valueOf(data.getInt("reward_type"))

            val heroFiManager = controller.masterUserManager.heroFiManager
            val repairedHeroes = SFSArray()
            val failedHeroes = SFSArray()

            for (heroId in heroList) {
                try {
                    val repairedHero = heroFiManager.repairShield(rewardType, heroId)
                    repairedHeroes.addSFSObject(repairedHero)
                } catch (e: Exception) {
                    val failObj = SFSObject()
                    failObj.putInt("hero_id", heroId)
                    failObj.putUtfString("error", e.message ?: "Repair failed")
                    failedHeroes.addSFSObject(failObj)
                }
            }

            // Reload rock
            val blockRewardManager = controller.masterUserManager.blockRewardManager
            blockRewardManager.loadUserBlockReward()

            val dataResponse = SFSObject()
            dataResponse.putSFSArray("repaired_heroes", repairedHeroes)
            dataResponse.putSFSArray("failed", failedHeroes)
            dataResponse.putSFSArray("rewards", blockRewardManager.toSfsArrays())
            sendSuccess(controller, requestId, dataResponse)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }
}
