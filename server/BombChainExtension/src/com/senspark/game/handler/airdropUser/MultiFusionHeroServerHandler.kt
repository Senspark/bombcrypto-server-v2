package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject

class MultiFusionHeroServerHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.MULTI_FUSION_HERO_SERVER

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.isAirdropUser()) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_AIRDROP, null)
        }

        try {
            val targetRarity = data.getInt("target")
            val heroList = data.getIntArray("hero_list").toList()

            if (targetRarity == 0)
                throw Exception("Not support fusion hero common")

            // Kiểm tra tất cả hero có chung rarity không
            val rarity = checkAndGetRarity(controller, heroList, targetRarity)

            val response = controller.masterUserManager.heroFiManager.multiFusionHeroServer(
                targetRarity, heroList, rarity
            )

            controller.masterUserManager.blockRewardManager.loadUserBlockReward()
            response.putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())
            sendSuccess(controller, requestId, response)

        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }

    private fun checkAndGetRarity(controller: IUserController, heroList: List<Int>, targetRarity: Int): Int {
        val rarity = controller.masterUserManager.heroFiManager.getHero(heroList.first(), controller.dataType)!!.rarity

        if (rarity >= targetRarity)
            throw Exception("Hero fusion invalid")
        heroList.map { heroId ->
            val hero = controller.masterUserManager.heroFiManager.getHero(heroId, controller.dataType)
                ?: throw Exception("Hero $heroId not exist")
            if (hero.rarity != rarity) {
                throw Exception("There is a hero with wrong rarity")
            }
        }
        return rarity
    }
}