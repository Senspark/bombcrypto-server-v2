package com.senspark.game.extension.helper

import com.senspark.common.cache.IMessengerService
import com.senspark.common.service.IScheduler
import com.senspark.common.utils.IServerLogger
import com.senspark.game.constant.StreamKeys
import com.senspark.game.data.manager.treassureHunt.ICoinRankingManager
import com.senspark.game.db.IUserDataAccess
import com.senspark.game.declare.SFSCommand
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.coroutines.ICoroutineScope
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.extension.modules.ServerType
import com.senspark.game.extension.schedulers.ExtensionSchedulerTon
import com.senspark.game.handler.airdropUser.*
import com.senspark.game.handler.ron.DepositRonHandler
import com.senspark.game.handler.ton.*
import com.senspark.game.handler.ton.club.*
import com.senspark.game.manager.IEnvManager
import com.senspark.game.manager.IUsersManager
import com.senspark.game.manager.ton.IClubManager

/**
 * Server Telegram
 */
class ServerInitializerTon(
    private val _services: GlobalServices
) : IServerInitializer {

    private val _netServices = _services.get<ISvServicesContainer>().get(ServerType.TON)

    override fun initHandlers(helper: AddRequestHandlerHelper) {
        helper.addRequestHandler(SFSCommand.COMPLETE_TASK_V2, CompleteTaskHandler::class.java)
        helper.addRequestHandler(SFSCommand.CLAIM_TASK_REWARD_V2, ClaimTaskRewardHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_USER_TASKS_V2, GetUserTasksHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_REFERRAL_DATA_V2, GetReferralDataHandler::class.java)
        helper.addRequestHandler(SFSCommand.CLAIM_REFERRAL_REWARD_V2, ClaimReferralRewardHandler::class.java)

        helper.addRequestHandler(SFSCommand.GET_CLUB_INFO_V2, GetClubInfoHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_ALL_CLUB_V2, GetAllClubHandler::class.java)
        helper.addRequestHandler(SFSCommand.JOIN_CLUB_V2, UserJoinClubHandler::class.java)
        helper.addRequestHandler(SFSCommand.LEAVE_CLUB_V2, UserLeaveClubHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_TOP_BID_CLUB_V2, GetTopBidClubHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_BID_PRICE_V2, GetBidPriceHandler::class.java)
        helper.addRequestHandler(SFSCommand.BOOST_CLUB_V2, BoostClubHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_INVOICE_DEPOSIT_TON_V2, DepositTonHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_OFFLINE_TH_MODE_REWARD_V2, GetOfflineRewardTHModeHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_HERO_OLD_SEASON, GetHeroServerOldSeasonHandler::class.java)
        helper.addRequestHandler(SFSCommand.REACTIVE_HOUSE_OLD_SEASON, ReactiveHouseOldSeasonHandler::class.java)
        helper.addRequestHandler(SFSCommand.JOIN_CLUB_V3, UserJoinClubHandlerV3::class.java)
        helper.addRequestHandler(SFSCommand.CREATE_CLUB_V3, UserCreateClubHandlerV3::class.java)
    }

    override fun initStreamListeners() {
        val envManager = _services.get<IEnvManager>()
        if (envManager.useStreamListener) {
            val messenger = _services.get<IMessengerService>()
            val logger = _netServices.get<IServerLogger>()
            val clubManager = _netServices.get<IClubManager>()
            val userDataAccess = _services.get<IUserDataAccess>()
            val usersManager = _netServices.get<IUsersManager>()
            val coroutineScope = _services.get<ICoroutineScope>()

            val tonDepositHandler = TonDepositResponseHandler(logger, userDataAccess, usersManager, coroutineScope)

            messenger.listen(StreamKeys.AP_TON_TRANSACTION) { message ->
                tonDepositHandler.handle(message.value)
                false
            }
            messenger.listen(StreamKeys.AP_CREATE_CLUB) { data ->
                clubManager.createClub(data.value)
                false
            }
            messenger.listen(StreamKeys.AP_JOIN_CLUB) { data ->
                clubManager.joinClub(data.value)
                false
            }
            messenger.listen(StreamKeys.AP_LEAVE_CLUB) { data ->
                clubManager.leaveClub(data.value)
                false
            }
        }
    }

    override fun initSchedulers() {
        val s = _services.get<IScheduler>()
        val logger = _netServices.get<IServerLogger>()
        val coinRankingManager = _netServices.get<ICoinRankingManager>()
        val clubManager = _netServices.get<IClubManager>()

        ExtensionSchedulerTon(s, logger, coinRankingManager, clubManager).initialize()
    }
}