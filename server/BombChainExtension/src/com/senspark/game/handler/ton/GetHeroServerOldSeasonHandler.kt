package com.senspark.game.handler.ton

import com.senspark.game.controller.IUserController
import com.senspark.game.data.manager.hero.IHeroBuilder
import com.senspark.game.data.model.nft.Hero
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.declare.SFSField
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class GetHeroServerOldSeasonHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.GET_HERO_OLD_SEASON

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.isAirdropUser()) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_AIRDROP, null)
        }
        try {
            val heroBuilder = controller.svServices.get<IHeroBuilder>()
            val heroes = when (controller.dataType) {
                EnumConstants.DataType.TON -> heroBuilder.getHeroesOldSeason(controller.userId, EnumConstants.HeroType.TON)
                EnumConstants.DataType.SOL -> heroBuilder.getHeroesOldSeason(controller.userId, EnumConstants.HeroType.SOL)
                EnumConstants.DataType.RON -> heroBuilder.getHeroesOldSeason(controller.userId, EnumConstants.HeroType.RON)
                EnumConstants.DataType.BAS -> heroBuilder.getHeroesOldSeason(controller.userId, EnumConstants.HeroType.BAS)
                else -> throw CustomException("Not support data type ${controller.dataType}")
            }

            val oldSeasonHeroes = SFSArray()
            heroes.forEach { oldSeasonHeroes.addSFSObject(heroToSfsObject(it)) }

            val result: ISFSObject = SFSObject()
            result.putSFSArray("old_season", oldSeasonHeroes)
            sendSuccess(controller, requestId, result)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }

    private fun heroToSfsObject(hero: Hero): ISFSObject {
        val sfsBomber: ISFSObject = SFSObject()
        // Do dưới client không sử dụng nên chỉ trả một số thông tin để hiện UI
        sfsBomber.putLong(SFSField.ID, hero.heroId.toLong())
        sfsBomber.putSFSObject("data", SFSObject().apply {
            putInt("playerType", hero.skin)
            putInt("playercolor", hero.color)
            putInt("rare", hero.rarity)
            putInt("bombDamage", hero.bombPower)
            putInt("speed", hero.speed)
            putInt("stamina", hero.stamina)
            putInt("bombNum", hero.bombCount)
            putInt("bombRange", hero.bombRange)
            putIntArray("abilities", hero.abilityList.items)
        })
        sfsBomber.putInt(SFSField.HeroType, hero.type.value)
        return sfsBomber
    }
}