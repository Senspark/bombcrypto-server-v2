package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.treassureHunt.ITreasureHuntConfigManager
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import kotlin.math.pow
import kotlin.math.roundToInt

class FusionHeroServerHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.FUSION_HERO_SERVER

    private val treasureHuntConfigManager = services.get<ITreasureHuntConfigManager>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.isAirdropUser()) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_AIRDROP, null)
        }

        try {
            val targetRarity = data.getInt("target")
            val heroList = data.getIntArray("hero_list").toList()

            if (targetRarity == 0)
                throw Exception("Not support fusion hero common")

            //Convert danh sách hero fusion sang rarity
            val rarityList = convertHeroListToRarityList(controller, heroList)

            //Có hero trong danh sách fusion có rarity >= targetRarity
            if (!heroFusionIsValid(targetRarity, rarityList))
                throw Exception("Hero fusion invalid")

            //Lấy % fusion thành công
            val percent = getPercentFusion(targetRarity, rarityList)

            //Tính price user phải trả
            val fee = treasureHuntConfigManager.getFusionFeeConfig(controller.dataType)[targetRarity]
            val priceToFusion = fee * (percent * 0.01)

            val response = controller.masterUserManager.heroFiManager.fusionHeroServer(
                targetRarity, heroList, percent, priceToFusion, controller.dataType
            )

            controller.masterUserManager.blockRewardManager.loadUserBlockReward()
            response.putSFSArray(SFSField.Rewards, controller.masterUserManager.blockRewardManager.toSfsArrays())
            sendSuccess(controller, requestId, response)

        } catch (ex: Exception) {
            sendExceptionError(controller, requestId, ex)
        }
    }

    private fun getPercentFusion(targetRarity: Int, heroRarityList: List<Int>): Int {
        var percent = 0.0

        for (rarity in heroRarityList) {
            val x = targetRarity - rarity
            percent += 25f / 4.0.pow((x - 1))
        }

        return percent.roundToInt()
    }

    private fun convertHeroListToRarityList(controller: IUserController, heroList: List<Int>): List<Int> {
        return heroList.map { heroId ->
            controller.masterUserManager.heroFiManager.getHero(heroId, controller.dataType)?.rarity
                ?: throw Exception("Hero $heroId not exist")
        }
    }

    private fun heroFusionIsValid(targetRarity: Int, heroListRarity: List<Int>): Boolean {
        return heroListRarity.none { it >= targetRarity }
    }
}