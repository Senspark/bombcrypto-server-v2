package com.senspark.game.handler.airdropUser

import com.senspark.game.controller.IUserController
import com.senspark.game.data.model.ServerHeroDetails
import com.senspark.game.db.IGameDataAccess
import com.senspark.game.declare.EnumConstants
import com.senspark.game.declare.ErrorCode
import com.senspark.game.declare.SFSCommand
import com.senspark.game.exception.CustomException
import com.senspark.game.handler.sol.BaseEncryptRequestHandler
import com.senspark.game.manager.hero.IUserHeroFiManager
import com.senspark.lib.data.manager.IGameConfigManager
import com.smartfoxserver.v2.entities.data.ISFSObject

/**
 * Handler for adding heroes to airdrop users.
 * This handler checks if a user is eligible for an airdrop and provides trial heroes if needed.
 */
class AddHeroAirdropHandler : BaseEncryptRequestHandler() {
    override val serverCommand = SFSCommand.ADD_HERO_FOR_AIRDROP_USER

    private val gameDataAccess = services.get<IGameDataAccess>()
    private val gameConfigManager = services.get<IGameConfigManager>()

    override fun handleGameClientRequest(controller: IUserController, requestId: Int, data: ISFSObject) {
        if (!controller.isAirdropUser()) {
            return sendError(controller, requestId, ErrorCode.NOT_USER_AIRDROP, null)
        }

        try {
            val dataType = controller.dataType
            val heroFiManager = controller.masterUserManager.heroFiManager
            
            // Get heroes for the appropriate blockchain
            val heroes = getHeroesForNetwork(heroFiManager, dataType)
            
            // Add trial heroes if user doesn't have any
            if (heroes.isEmpty()) {
                addTrialHeroes(controller, dataType)
            }

            // Prepare response with hero data
            val result = heroFiManager.sendServerHeroToClient(dataType)
            
            // Add hero count to response
            val heroType = getHeroTypeForDataType(dataType)
            result.putInt(
                "heroes_size",
                gameDataAccess.getUserQuantityHeroes(controller.userId, heroType, dataType)
            )
            
            sendSuccess(controller, requestId, result)
        } catch (e: Exception) {
            sendExceptionError(controller, requestId, e)
        }
    }

    /**
     * Gets the appropriate hero list based on the blockchain network type.
     */
    private fun getHeroesForNetwork(heroFiManager: IUserHeroFiManager, dataType: EnumConstants.DataType): List<com.senspark.game.data.model.nft.Hero> {
        return when (dataType) {
            EnumConstants.DataType.TON -> heroFiManager.getAllHeroTon()
            EnumConstants.DataType.RON -> heroFiManager.getAllHeroRon()
            EnumConstants.DataType.SOL -> heroFiManager.getAllHeroSol()
            EnumConstants.DataType.BAS -> heroFiManager.getAllHeroBas()
            EnumConstants.DataType.VIC -> heroFiManager.getAllHeroVic()
            else -> throw CustomException("Unsupported network: ${dataType.name}")
        }
    }

    /**
     * Adds trial heroes for new airdrop users.
     */
    private fun addTrialHeroes(controller: IUserController, dataType: EnumConstants.DataType) {
        val listRarities = gameConfigManager.newUserAirdropGiftHero
        val serverHeroDetails = listRarities.map { rarity ->
            ServerHeroDetails.generateByRarity(1, dataType, rarity)
        }.toMutableList()
        
        controller.masterUserManager.heroFiManager.addHeroesServer(serverHeroDetails)
    }

    /**
     * Maps DataType to the corresponding HeroType.
     */
    private fun getHeroTypeForDataType(dataType: EnumConstants.DataType): EnumConstants.HeroType {
        return when (dataType) {
            EnumConstants.DataType.TON -> EnumConstants.HeroType.TON
            EnumConstants.DataType.RON -> EnumConstants.HeroType.RON
            EnumConstants.DataType.SOL -> EnumConstants.HeroType.SOL
            EnumConstants.DataType.BAS -> EnumConstants.HeroType.BAS
            EnumConstants.DataType.VIC -> EnumConstants.HeroType.VIC
            else -> throw CustomException("Unsupported network for hero type: ${dataType.name}")
        }
    }
}