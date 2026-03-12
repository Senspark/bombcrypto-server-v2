package com.senspark.game.extension.helper

import com.senspark.common.cache.IMessengerService
import com.senspark.common.constant.PVPInternalCommand
import com.senspark.common.utils.IServerLogger
import com.senspark.game.api.GameInternalMessageHandler
import com.senspark.game.constant.StreamKeys
import com.senspark.game.data.manager.IMasterDataManager
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.SFSCommand
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.extension.modules.ServerType
import com.senspark.game.extension.schedulers.ExtensionSchedulerVic
import com.senspark.game.handler.vic.DepositVicHandler
import com.senspark.game.handler.vic.VicDepositResponseHandler
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.pvp.IPvpResultManager
import com.senspark.game.pvp.manager.IPvpQueueManager
import com.senspark.lib.data.manager.IGameConfigManager

/**
 * Server VIC
 */
class ServerInitializerVic(
    private val _services: GlobalServices
) : IServerInitializer {

    private val _netServices = _services.get<ISvServicesContainer>().get(ServerType.VIC)

    override fun initHandlers(helper: AddRequestHandlerHelper) {
        helper.addRequestHandler(SFSCommand.GET_INVOICE_DEPOSIT_VIC, DepositVicHandler::class.java)
    }

    override fun initStreamListeners() {
        val envManager = _services.get<IEnvManager>()
        if (envManager.useStreamListener) {
            val messenger = _services.get<IMessengerService>()
            val logger = _netServices.get<IServerLogger>()
            val usersManager = _netServices.get<IUsersManager>()
            val coroutineScope = _services.get<ICoroutineScope>()
            val userDataAccess = _services.get<IUserDataAccess>()
            val gameConfig = _services.get<IGameConfigManager>()
            val queueManager = _netServices.get<IPvpQueueManager>()
            val masterDataManager = _netServices.get<IMasterDataManager>()
            val pvpResultManager = _netServices.get<IPvpResultManager>()

            val vicDepositHandler = VicDepositResponseHandler(logger, userDataAccess, usersManager, coroutineScope)
            messenger.listen(StreamKeys.AP_VIC_TRANSACTION) { message ->
                vicDepositHandler.handle(message.value)
                false
            }
            
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
        }
    }

    override fun initSchedulers() {
        ExtensionSchedulerVic(_services, _netServices).initialize()
    }
}