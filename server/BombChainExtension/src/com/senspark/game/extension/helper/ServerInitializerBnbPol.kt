package com.senspark.game.extension.helper

import com.senspark.common.cache.IMessengerService
import com.senspark.common.constant.PVPInternalCommand
import com.senspark.game.api.GameInternalMessageHandler
import com.senspark.game.api.IVerifyAdApiManager
import com.senspark.game.constant.StreamKeys
import com.senspark.game.data.manager.IMasterDataManager
import com.senspark.game.declare.SFSCommand
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.extension.modules.ServerType
import com.senspark.game.extension.schedulers.ExtensionSchedulerBnbPol
import com.senspark.game.handler.nft.*
import com.senspark.game.handler.rock.*
import com.senspark.game.handler.shield.GetRepairShieldConfigHandler
import com.senspark.game.handler.shield.RepairShieldHandler
import com.senspark.game.handler.stake.GetMinStakeHeroHandler
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.blockChain.IBlockchainResponseManager
import com.senspark.game.pvp.IPvpResultManager
import com.senspark.game.pvp.manager.IPvpQueueManager
import com.senspark.game.service.IAllHeroesFiManager
import com.senspark.lib.data.manager.IGameConfigManager

/**
 * Server BNB/POLYGON
 */
class ServerInitializerBnbPol(
    private val _services: GlobalServices
) : IServerInitializer {

    private val _netServices = _services.get<ISvServicesContainer>().get(ServerType.BNB_POL)

    override fun initHandlers(helper: AddRequestHandlerHelper) {
        // th mode
        helper.addRequestHandler(SFSCommand.SYNC_BOMBERMAN_V2, SyncBombermanHandler::class.java)
        helper.addRequestHandler(SFSCommand.SYNC_DEPOSITED_V2, SyncDepositedHandler::class.java)
        helper.addRequestHandler(SFSCommand.REPAIR_SHIELD_V2, RepairShieldHandler::class.java)
        helper.addRequestHandler(SFSCommand.BUY_ROCK_V2, UserBuyRockPackHandler::class.java)
        helper.addRequestHandler(SFSCommand.CREATE_ROCK_V2, CreateRockHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_BURN_HERO_CONFIG_V2, GetBurnHeroConfigHandler::class.java)
        helper.addRequestHandler(SFSCommand.UPGRADE_SHIELD_LEVEL_V2, UpgradeShieldLevelHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_UPGRADE_SHIELD_CONFIG_V2, GetUpgradeShieldConfigHandler::class.java)
        helper.addRequestHandler(SFSCommand.CHECK_BOMBER_STAKE_V2, CheckBomberStakeHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_MIN_STAKE_HERO_V2, GetMinStakeHeroHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_ROCK_PACK_CONFIG_V2, GetRockConfigPackHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_REPAIR_SHIELD_CONFIG_V2, GetRepairShieldConfigHandler::class.java)

        // new handler not wait api response
        helper.addRequestHandler(SFSCommand.SYNC_BOMBERMAN_V3, SyncBombermanHandlerV3::class.java)
        helper.addRequestHandler(SFSCommand.SYNC_DEPOSITED_V3, SyncDepositedHandlerV3::class.java)
    }

    override fun initStreamListeners() {
        val envManager = _services.get<IEnvManager>()
        if (envManager.useStreamListener) {
            val messenger = _services.get<IMessengerService>()
            val gameConfig = _services.get<IGameConfigManager>()
            val usersManager = _netServices.get<IUsersManager>()
            val heroes = _netServices.get<IAllHeroesFiManager>()
            val queueManager = _netServices.get<IPvpQueueManager>()
            val masterDataManager = _netServices.get<IMasterDataManager>()
            val pvpResultManager = _netServices.get<IPvpResultManager>()
            val adsManager = _netServices.get<IVerifyAdApiManager>()

            val internalMessageHandler = GameInternalMessageHandler(
                queueManager,
                masterDataManager,
                pvpResultManager,
                messenger,
                usersManager,
                gameConfig
            )

            messenger.listen(StreamKeys.AP_PVP_MATCH_FOUND_STR) { data ->
                internalMessageHandler.handle(PVPInternalCommand.PVP_FOUND_MATCH, data.value)
                false
            }
            messenger.listen(StreamKeys.SV_PVP_MATCH_FINISHED_STR) { data ->
                internalMessageHandler.handle(PVPInternalCommand.PVP_END_MATCH, data.value)
                false
            }
            messenger.listen(StreamKeys.AP_BL_HERO_STAKE_STR) { data ->
                heroes.processHeroStake(data.value)
                false
            }
            messenger.listen(StreamKeys.AP_MONETIZATION_ADS_VERIFY) { data ->
                adsManager.processAdsReward(data.value)
                false
            }

             val blockchainManager = _netServices.get<IBlockchainResponseManager>()
            // Listen stream key blockchain for sync house, sync hero, sync deposit
            messenger.listen(StreamKeys.AP_BL_SYNC_HERO) { data ->
                blockchainManager.listenSyncHero(data.value)
            }
            messenger.listen(StreamKeys.AP_BL_SYNC_HOUSE) { data ->
                blockchainManager.listenSyncHouse(data.value)
            }
            messenger.listen(StreamKeys.AP_BL_SYNC_DEPOSIT) { data ->
                blockchainManager.listenSyncDeposit(data.value)
            }
        }
    }

    override fun initSchedulers() {
        ExtensionSchedulerBnbPol(_services, _netServices).initialize()
    }
}