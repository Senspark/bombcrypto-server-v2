package com.senspark.game.extension.helper

import com.senspark.common.cache.IMessengerService
import com.senspark.common.service.IScheduler
import com.senspark.common.utils.ILogger
import com.senspark.common.utils.IServerLogger
import com.senspark.game.constant.StreamKeys
import com.senspark.game.data.manager.treassureHunt.ICoinRankingManager
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.SFSCommand
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.extension.modules.ServerType
import com.senspark.game.extension.schedulers.ExtensionSchedulerSol
import com.senspark.game.handler.solana.DepositSolHandler
import com.senspark.game.handler.solana.SolDepositResponseHandler
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager

/**
 * Server Solana
 */
class ServerInitializerSol(
    private val _services: GlobalServices
) : IServerInitializer {

    private val _netServices = _services.get<ISvServicesContainer>().get(ServerType.SOL)
    
    override fun initHandlers(helper: AddRequestHandlerHelper) {
        helper.addRequestHandler(SFSCommand.GET_INVOICE_DEPOSIT_SOL, DepositSolHandler::class.java)
    }

    override fun initStreamListeners() {
        val envManager = _services.get<IEnvManager>()
        if (envManager.useStreamListener) {
            val messenger = _services.get<IMessengerService>()
            val logger = _netServices.get<IServerLogger>()
            val userDataAccess = _services.get<IUserDataAccess>()
            val usersManager = _netServices.get<IUsersManager>()
            val coroutineScope = _services.get<ICoroutineScope>()

            val solDepositHandler = SolDepositResponseHandler(logger, userDataAccess, usersManager, coroutineScope)
            
            messenger.listen(StreamKeys.AP_SOL_TRANSACTION) { message ->
                solDepositHandler.handle(message.value)
                false
            }
        }
    }

    override fun initSchedulers() {
        val s = _services.get<IScheduler>()
        val logger = _netServices.get<IServerLogger>()
        val coinRankingManager = _netServices.get<ICoinRankingManager>()
        ExtensionSchedulerSol(s, logger, coinRankingManager).initialize()
    }
}