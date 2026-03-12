package com.senspark.game.service

import com.senspark.common.cache.ICacheService
import com.senspark.common.service.IGlobalService
import com.senspark.common.service.IScheduler
import com.senspark.common.utils.IServerLogger
import com.senspark.game.constant.CachedKeys.Companion.SV_HANDLER_LOGGER
import com.senspark.game.controller.IUserController
import com.senspark.game.declare.SFSCommand
import kotlin.time.Duration.Companion.minutes

private const val SCHEDULER_NAME = "HandlerLogger"

interface IHandlerLogger : IGlobalService {
    fun dispose()
    fun canLog(handlerName: String): Boolean
    fun log(userController: IUserController, handlerName: String, message: String)
}

/**
 * SINGLETON
 */
class HandlerLogger(
    private val _scheduler: IScheduler,
    private val _cache: ICacheService
) : IHandlerLogger {
    private val _handlerNames = mutableSetOf<String>()
    private val _additionHandlerNames = mutableSetOf<String>()
    private val _refreshInterval = 5.minutes

    override fun initialize() {
        _handlerNames.add(SFSCommand.SYNC_BOMBERMAN_V2)
        _handlerNames.add(SFSCommand.SYNC_HOUSE_V2)
        _handlerNames.add(SFSCommand.SYNC_DEPOSITED_V2)
        _handlerNames.add(SFSCommand.GET_BLOCK_MAP_V2)
        _handlerNames.add(SFSCommand.START_PVE_V2)
        _handlerNames.add(SFSCommand.STOP_PVE_V2)
        _handlerNames.add(SFSCommand.GET_REWARD_V2)
        _handlerNames.add(SFSCommand.GO_SLEEP_V2)
        _handlerNames.add(SFSCommand.GO_WORK_V2)
        _handlerNames.add(SFSCommand.GO_HOME_V2)
        _handlerNames.add(SFSCommand.CHANGE_BBM_STAGE_V2)
        _handlerNames.add(SFSCommand.ACTIVE_HOUSE_V2)
        _handlerNames.add(SFSCommand.ACTIVE_BOMBER_V2)
        _handlerNames.add(SFSCommand.ACTIVE_HERO_TR_V2)
        _handlerNames.add(SFSCommand.START_STORY_EXPLODE_V2)
        _handlerNames.add(SFSCommand.GET_NEWCOMER_GIFTS_V2)
        _handlerNames.add(SFSCommand.APPROVE_CLAIM_V2)
        _handlerNames.add(SFSCommand.CONFIRM_CLAIM_REWARD_SUCCESS_V2)
        _handlerNames.add(SFSCommand.USER_STAKE_V2)
        _handlerNames.add(SFSCommand.USER_WITHDRAW_STAKE_V2)
        _handlerNames.add(SFSCommand.GET_PVP_RANKING_V2)
        _handlerNames.add(SFSCommand.CLAIM_MONTHLY_REWARD_V2)
        _handlerNames.add(SFSCommand.OPEN_SKIN_CHEST_V2)
        _handlerNames.add(SFSCommand.ACTIVE_SKIN_CHEST_V3)
        _handlerNames.add(SFSCommand.REPAIR_SHIELD_V2)
        _handlerNames.add(SFSCommand.BUY_AUTO_MINE_V2)
        _handlerNames.add(SFSCommand.BUY_ROCK_V2)
        _handlerNames.add(SFSCommand.START_AUTO_MINE_V2)
        _handlerNames.add(SFSCommand.BUY_ITEM_MARKETPLACE_V2)
        _handlerNames.add(SFSCommand.CANCEL_SELL_ITEM_MARKETPLACE_V2)
        _handlerNames.add(SFSCommand.SELL_ITEM_MARKETPLACE_V2)
        _handlerNames.add(SFSCommand.EDIT_ITEM_MARKETPLACE_V2)
        _handlerNames.add(SFSCommand.SWAP_TOKEN_V2)
        _handlerNames.add(SFSCommand.PREVIEW_TOKEN_V2)
        _handlerNames.add(SFSCommand.START_OPENING_GACHA_CHEST_V2)
        _handlerNames.add(SFSCommand.OPEN_GACHA_CHEST_V2)
        _handlerNames.add(SFSCommand.BUY_GACHA_CHEST_V2)
        _handlerNames.add(SFSCommand.BUY_GACHA_CHEST_SLOT_V2)
        _handlerNames.add(SFSCommand.BUY_COSTUME_ITEM_V2)
        _handlerNames.add(SFSCommand.SKIP_OPEN_CHEST_TIME_BY_GEM_V2)
        _handlerNames.add(SFSCommand.SKIP_OPEN_CHEST_TIME_BY_ADS_V2)
        _handlerNames.add(SFSCommand.BUY_GOLD_V2)
        _handlerNames.add(SFSCommand.BUY_GEM_V2)
        _handlerNames.add(SFSCommand.BUY_PACK_V2)
        _handlerNames.add(SFSCommand.WATCHING_DAILY_MISSION_ADS_V2)
        _handlerNames.add(SFSCommand.TAKE_DAILY_MISSION_REWARD_V2)
        _handlerNames.add(SFSCommand.ADD_HERO_FOR_AIRDROP_USER)
        _handlerNames.add(SFSCommand.GET_COIN_RANKING_V2)
        _handlerNames.add(SFSCommand.BUY_HOUSE_SERVER)
        _handlerNames.add(SFSCommand.BUY_HERO_SERVER)
        _handlerNames.add(SFSCommand.GRIND_HEROES_V2)
        _handlerNames.add(SFSCommand.UPGRADE_CRYSTAL_V2)
        _handlerNames.add(SFSCommand.GET_OFFLINE_REWARDS_V2)
        _handlerNames.add(SFSCommand.CLAIM_OFFLINE_REWARDS_V2)
        _handlerNames.add(SFSCommand.UPGRADE_HERO_TR_V2)
        _handlerNames.add(SFSCommand.MARK_ITEM_VIEWED_V2)
        _handlerNames.add(SFSCommand.CHECK_BOMBER_STAKE_V2)
        _handlerNames.add(SFSCommand.GET_MIN_STAKE_HERO_V2)
        _handlerNames.add(SFSCommand.ADMIN_COMMANDS)
        _handlerNames.add(SFSCommand.MODERATOR_COMMANDS)
        _handlerNames.add(SFSCommand.SEND_MESSAGE_SLACK_V2)
//        _handlerNames.add(SFSCommand.START_EXPLODE_V5)
        _handlerNames.add(SFSCommand.CREATE_ROCK_V2)
        _handlerNames.add(SFSCommand.UPGRADE_SHIELD_LEVEL_V2)
        _handlerNames.add(SFSCommand.GET_INVOICE_DEPOSIT_TON)
        _handlerNames.add(SFSCommand.GET_INVOICE_DEPOSIT_SOL)
        _handlerNames.add(SFSCommand.GET_OFFLINE_TH_MODE_REWARD_V2)
        _handlerNames.add(SFSCommand.CLAIM_PVP_MATCH_REWARD_V2)
        _handlerNames.add(SFSCommand.COMPLETE_TASK_V2)
        _handlerNames.add(SFSCommand.CLAIM_TASK_REWARD_V2)
        _handlerNames.add(SFSCommand.ENTER_ADVENTURE_DOOR_V2)
        _handlerNames.add(SFSCommand.GET_BONUS_REWARD_ADVENTURE_V3)
        _handlerNames.add(SFSCommand.TAKE_ADVENTURE_ITEM_V2)
        _handlerNames.add(SFSCommand.USE_ADVENTURE_BOOSTER_V2)

        _scheduler.schedule(SCHEDULER_NAME, 0, _refreshInterval.inWholeMilliseconds.toInt()) {
            val additions = _cache.readSet(SV_HANDLER_LOGGER)
            _additionHandlerNames.clear()
            _additionHandlerNames.addAll(additions)
        }
    }

    override fun dispose() {
        _scheduler.clear(SCHEDULER_NAME)
    }

    override fun canLog(handlerName: String): Boolean {
        return _handlerNames.contains(handlerName) || _additionHandlerNames.contains(handlerName)
    }

    override fun log(controller: IUserController, handlerName: String, message: String) {
        if (canLog(handlerName)) {
            val logger = controller.svServices.get<IServerLogger>()
            logger.log("[$handlerName] ${controller.userName}: $message")
        }
    }
}