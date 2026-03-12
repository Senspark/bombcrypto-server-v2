package com.senspark.game.extension.helper

import com.senspark.common.cache.IMessengerService
import com.senspark.game.constant.StreamKeys
import com.senspark.game.declare.SFSCommand
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.modules.ISvServicesContainer
import com.senspark.game.extension.schedulers.ExtensionSchedulerAll
import com.senspark.game.handler.adventure.AdventureReviveHeroHandler
import com.senspark.game.handler.adventure.EnemyTakeDamageHandler
import com.senspark.game.handler.adventure.EnterDoorHandler
import com.senspark.game.handler.adventure.GetAdventureLevelHandler
import com.senspark.game.handler.adventure.GetAdventureMapHandler
import com.senspark.game.handler.adventure.GetBonusRewardAdventureHandler
import com.senspark.game.handler.adventure.HeroTakeDamageHandler
import com.senspark.game.handler.adventure.SpawnEnemyHandler
import com.senspark.game.handler.adventure.StartAdventureExplodeHandler
import com.senspark.game.handler.adventure.TakeAdventureItemHandler
import com.senspark.game.handler.adventure.UseAdventureBoosterHandler
import com.senspark.game.handler.airdropUser.*
import com.senspark.game.handler.clientLog.SendClientLogHandler
import com.senspark.game.handler.convertToken.ConvertTokenHandler
import com.senspark.game.handler.convertToken.GetSwapTokenConfig
import com.senspark.game.handler.convertToken.PreviewTokenHandler
import com.senspark.game.handler.dailyMission.GetDailyMissionHandler
import com.senspark.game.handler.dailyMission.TakeMissionRewardHandler
import com.senspark.game.handler.dailyMission.WatchingDailyAdsHandler
import com.senspark.game.handler.dailyTask.ClaimDailyTaskHandler
import com.senspark.game.handler.dailyTask.GetDailyTaskConfigHandler
import com.senspark.game.handler.dailyTask.GetUserDailyProgressHandler
import com.senspark.game.handler.data.CheckNoAdsHandler
import com.senspark.game.handler.data.DeleteUserHandler
import com.senspark.game.handler.data.GetStartGameConfigHandler
import com.senspark.game.handler.data.UserLinkHandler
import com.senspark.game.handler.gacha.BuyGachaChestHandler
import com.senspark.game.handler.gacha.BuyGachaChestSlotHandler
import com.senspark.game.handler.gacha.GetGachaChestShopHandler
import com.senspark.game.handler.gacha.GetGachaChestsHandler
import com.senspark.game.handler.gacha.OpenGachaChestHandler
import com.senspark.game.handler.gacha.StartOpeningGachaChestHandler
import com.senspark.game.handler.heroTR.ActiveHeroTRHandler
import com.senspark.game.handler.heroTR.GetHeroUpgradePowerHandler
import com.senspark.game.handler.heroTR.GetHeroesTraditionalHandler
import com.senspark.game.handler.heroTR.GetUpgradeConfigHandler
import com.senspark.game.handler.iapshop.BuyGoldHandler
import com.senspark.game.handler.iapshop.GetFreeGemsHandler
import com.senspark.game.handler.iapshop.GetFreeGoldsHandler
import com.senspark.game.handler.iapshop.GetFreeRewardConfigsHandler
import com.senspark.game.handler.iapshop.GetGemShopHandler
import com.senspark.game.handler.iapshop.GetGoldShopHandler
import com.senspark.game.handler.iapshop.GetPackShopHandler
import com.senspark.game.handler.iapshop.SendMessageSlackHandler
import com.senspark.game.handler.iapshop.UserBuyGemHandler
import com.senspark.game.handler.iapshop.UserBuyPackHandler
import com.senspark.game.handler.luckyWheel.GetLuckyWheelReward
import com.senspark.game.handler.marketplace.BuyItemHandler
import com.senspark.game.handler.marketplace.CancelSellItemHandler
import com.senspark.game.handler.marketplace.EditItemHandler
import com.senspark.game.handler.marketplace.GetItemMarketplaceHandler
import com.senspark.game.handler.marketplace.GetOnSellHandler
import com.senspark.game.handler.marketplace.GetUserInventoryHandler
import com.senspark.game.handler.marketplace.SellItemHandler
import com.senspark.game.handler.misc.GetServerInfoHandler
import com.senspark.game.handler.moderator.AdminCommandStreamProcessor
import com.senspark.game.handler.newMarket.BuyItemMarketHandler
import com.senspark.game.handler.newMarket.CancelOrderItemMarketHandler
import com.senspark.game.handler.newMarket.CancelSellItemMarketHandler
import com.senspark.game.handler.newMarket.EditItemMarketHandler
import com.senspark.game.handler.newMarket.GetCurrentMarketMinPriceHandler
import com.senspark.game.handler.newMarket.GetMarketConfigHandler
import com.senspark.game.handler.newMarket.GetMyItemMarket
import com.senspark.game.handler.newMarket.OrderItemMarketHandler
import com.senspark.game.handler.newMarket.SellItemMarketHandler
import com.senspark.game.handler.nonFi.GetNewcomerGiftsHandler
import com.senspark.game.handler.onBoarding.GetOnBoardingConfigHandler
import com.senspark.game.handler.onBoarding.UpdateUserOnBoardingHandler
import com.senspark.game.handler.pvp.*
import com.senspark.game.handler.request.ApproveClaimWithoutConfirmHandler
import com.senspark.game.handler.request.ConfirmClaimHandler
import com.senspark.game.handler.request.GetSkinInventoryHandler
import com.senspark.game.handler.room.KeepAliveRequestHandler
import com.senspark.game.handler.shop.BuyCostumeItemHandler
import com.senspark.game.handler.shop.GetCostumeShopHandler
import com.senspark.game.handler.skinPVP.ActiveSkinChestHandler
import com.senspark.game.handler.skinPVP.OpenSkinChestHandler
import com.senspark.game.handler.upgradeHero.GetCrystalHandler
import com.senspark.game.handler.upgradeHero.GrindHeroHandler
import com.senspark.game.handler.upgradeHero.UpgradeCrystalHandler
import com.senspark.game.handler.upgradeHero.UpgradeHeroTrHandler
import com.senspark.game.handler.user.GetOtherUserInfoHandler
import com.senspark.game.handler.user.MarkItemViewedHandler
import com.senspark.game.manager.IUsersManager
import com.smartfoxserver.v2.entities.Zone
import com.smartfoxserver.v2.extensions.SFSExtension

/**
 * Cần thiết cho bất kỳ loại server nào
 */
class ServerInitializerAll(
    private val _services: GlobalServices,
    private val _zone: Zone,
    private val _extension: SFSExtension,
) : IServerInitializer {

    override fun initHandlers(helper: AddRequestHandlerHelper) {
        helper.addRequestHandler(SFSCommand.CMD_KEEP_ALIVE, KeepAliveRequestHandler::class.java)
        helper.addRequestHandler(SFSCommand.PING_PONG, KeepAliveRequestHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_TH_DATA_CONFIG_V2, GetTreasureHuntConfigV2Handler::class.java)
        helper.addRequestHandler(SFSCommand.SYNC_HOUSE_V2, SyncHouseV2Handler::class.java)
        helper.addRequestHandler(SFSCommand.GET_BLOCK_MAP_V2, GetBlockMapV2Handler::class.java)
        helper.addRequestHandler(SFSCommand.START_PVE_V2, StartPVEV2Handler::class.java)
        helper.addRequestHandler(SFSCommand.STOP_PVE_V2, StopPVEV2Handler::class.java)
        helper.addRequestHandler(SFSCommand.GET_REWARD_V2, GetRewardV2Handler::class.java)
        helper.addRequestHandler(SFSCommand.GO_SLEEP_V2, GoSleepV2Handler::class.java)
        helper.addRequestHandler(SFSCommand.GO_WORK_V2, GoWorkV2Handler::class.java)
        helper.addRequestHandler(SFSCommand.GO_HOME_V2, GoHomeV2Handler::class.java)
        helper.addRequestHandler(SFSCommand.CHANGE_BBM_STAGE_V2, ChangeBomberManStageV2Handler::class.java)
        helper.addRequestHandler(SFSCommand.CHANGE_BBM_STAGE_V3, ChangeBomberManStageV3Handler::class.java)
        helper.addRequestHandler(SFSCommand.ACTIVE_HOUSE_V2, ActiveHouseV2Handler::class.java)
        helper.addRequestHandler(SFSCommand.ACTIVE_BOMBER_V2, ActiveBomberV2Handler::class.java)
        helper.addRequestHandler(SFSCommand.GET_ACTIVE_BOMBER_V2, GetActiveBomberV2Handler::class.java)
        helper.addRequestHandler(SFSCommand.BUY_AUTO_MINE_V2, UserBuyAutoMineV2Handler::class.java)
        helper.addRequestHandler(SFSCommand.START_AUTO_MINE_V2, UserStartAutoMineV2Handler::class.java)
        helper.addRequestHandler(SFSCommand.AUTO_MINE_PRICE_V2, UserAutoMinePackagePriceV2Handler::class.java)
        helper.addRequestHandler(SFSCommand.START_EXPLODE_V5, StartExplodeV5Handler::class.java)
        helper.addRequestHandler(SFSCommand.SEND_CLIENT_LOG, SendClientLogHandler::class.java)

        // pvp
        helper.addRequestHandler(SFSCommand.GET_PVP_HISTORY_V2, GetPvPHistoryHandler::class.java)
        helper.addRequestHandler(SFSCommand.SYNC_PVP_HERO_ENERGY_V2, SyncPvPHandler::class.java)
        helper.addRequestHandler(SFSCommand.JOIN_PVP_QUEUE_V2, PvpJoinQueueHandler::class.java)
        helper.addRequestHandler(SFSCommand.LEAVE_PVP_QUEUE_V2, PvpLeaveQueueHandler::class.java)
        helper.addRequestHandler(SFSCommand.SYNC_PVP_CONFIG_V2, SyncPvPConfigHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_USER_PVP_BOOSTERS_V2, GetUserPvpBoosterHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_PVP_RANKING_V2, GetPvpRankingHandler::class.java)
        helper.addRequestHandler(SFSCommand.CLAIM_MONTHLY_REWARD_V2, ClaimPvpRewardHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_PVP_SERVER_CONFIGS_V2, GetPVPServerConfigsHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_BONUS_REWARD_PVP_V3, GetBonusRewardPvpHandler::class.java)
        helper.addRequestHandler(SFSCommand.KEEP_JOINING_PVP_QUEUE_V2, PvpKeepJoiningQueueHandler::class.java)
        helper.addRequestHandler(SFSCommand.CLAIM_PVP_MATCH_REWARD_V2, ClaimPvpMatchRewardHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_RANK_INFO_V2, GetRankInfoHandler::class.java)

        // hero tr
        helper.addRequestHandler(SFSCommand.ACTIVE_HERO_TR_V2, ActiveHeroTRHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_HEROES_TRADITIONAL_V3, GetHeroesTraditionalHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_HERO_UPGRADE_POWER_V2, GetHeroUpgradePowerHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_UPGRADE_CONFIG_V2, GetUpgradeConfigHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_CRYSTALS_V2, GetCrystalHandler::class.java)
        helper.addRequestHandler(SFSCommand.GRIND_HEROES_V2, GrindHeroHandler::class.java)
        helper.addRequestHandler(SFSCommand.UPGRADE_CRYSTAL_V2, UpgradeCrystalHandler::class.java)
        helper.addRequestHandler(SFSCommand.UPGRADE_HERO_TR_V2, UpgradeHeroTrHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_ON_BOARDING_CONFIG, GetOnBoardingConfigHandler::class.java)
        helper.addRequestHandler(SFSCommand.UPDATE_USER_ON_BOARDING, UpdateUserOnBoardingHandler::class.java)

        // adventure mode
        helper.addRequestHandler(SFSCommand.GET_ADVENTURE_LEVEL_DETAIL_V2, GetAdventureLevelHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_ADVENTURE_MAP_V2, GetAdventureMapHandler::class.java)
        helper.addRequestHandler(SFSCommand.ENTER_ADVENTURE_DOOR_V2, EnterDoorHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_BONUS_REWARD_ADVENTURE_V3, GetBonusRewardAdventureHandler::class.java)
        helper.addRequestHandler(SFSCommand.TAKE_ADVENTURE_ITEM_V2, TakeAdventureItemHandler::class.java)
        helper.addRequestHandler(SFSCommand.USE_ADVENTURE_BOOSTER_V2, UseAdventureBoosterHandler::class.java)
        helper.addRequestHandler(SFSCommand.ADVENTURE_REVIVE_HERO_V2, AdventureReviveHeroHandler::class.java)
        helper.addRequestHandler(SFSCommand.ENEMY_TAKE_DAMAGE_V2, EnemyTakeDamageHandler::class.java)
        helper.addRequestHandler(SFSCommand.START_STORY_EXPLODE_V2, StartAdventureExplodeHandler::class.java)
        helper.addRequestHandler(SFSCommand.HERO_TAKE_DAMAGE_V2, HeroTakeDamageHandler::class.java)
        helper.addRequestHandler(SFSCommand.SPAWN_ENEMY_V2, SpawnEnemyHandler::class.java)

        // old p2p marketplace, support old client, remove in future
        helper.addRequestHandler(SFSCommand.BUY_ITEM_MARKETPLACE_V2, BuyItemHandler::class.java)
        helper.addRequestHandler(SFSCommand.CANCEL_SELL_ITEM_MARKETPLACE_V2, CancelSellItemHandler::class.java)
//        helper.addRequestHandler(SFSCommand.GET_ACTIVITY_MARKETPLACE_V2, GetActivityHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_DASHBOARD_MARKETPLACE_V2, GetUserInventoryHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_ITEM_MARKETPLACE_V2, GetItemMarketplaceHandler::class.java)
        helper.addRequestHandler(SFSCommand.SELL_ITEM_MARKETPLACE_V2, SellItemHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_ON_SELL_V2, GetOnSellHandler::class.java)
        helper.addRequestHandler(SFSCommand.EDIT_ITEM_MARKETPLACE_V2, EditItemHandler::class.java)

        // p2p marketplace v3
        helper.addRequestHandler(SFSCommand.ORDER_ITEM_MARKET_V3, OrderItemMarketHandler::class.java)
        helper.addRequestHandler(SFSCommand.CANCEL_ORDER_ITEM_MARKET_V3, CancelOrderItemMarketHandler::class.java)
        helper.addRequestHandler(SFSCommand.BUY_ITEM_MARKET_V3, BuyItemMarketHandler::class.java)
        helper.addRequestHandler(SFSCommand.SELL_ITEM_MARKET_V3, SellItemMarketHandler::class.java)
        helper.addRequestHandler(SFSCommand.EDIT_ITEM_MARKET_V3, EditItemMarketHandler::class.java)
        helper.addRequestHandler(SFSCommand.CANCEL_SELL_ITEM_MARKET_V3, CancelSellItemMarketHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_MY_ITEM_MARKET_V3, GetMyItemMarket::class.java)
        helper.addRequestHandler(SFSCommand.GET_MARKET_CONFIG_V3, GetMarketConfigHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_CURRENT_MARKET_MIN_PRICE_V3, GetCurrentMarketMinPriceHandler::class.java)

        // gacha
        helper.addRequestHandler(SFSCommand.GET_GACHA_CHESTS_V2, GetGachaChestsHandler::class.java)
        helper.addRequestHandler(SFSCommand.START_OPENING_GACHA_CHEST_V2, StartOpeningGachaChestHandler::class.java)
        helper.addRequestHandler(SFSCommand.OPEN_GACHA_CHEST_V2, OpenGachaChestHandler::class.java)
        helper.addRequestHandler(SFSCommand.BUY_GACHA_CHEST_V2, BuyGachaChestHandler::class.java)
        helper.addRequestHandler(SFSCommand.BUY_GACHA_CHEST_SLOT_V2, BuyGachaChestSlotHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_GACHA_CHEST_SHOP_V2, GetGachaChestShopHandler::class.java)
//        helper.addRequestHandler(SFSCommand.SKIP_OPEN_CHEST_TIME_BY_GEM_V2, SkipOpenChestTimeByGemHandler::class.java)
//        helper.addRequestHandler(SFSCommand.SKIP_OPEN_CHEST_TIME_BY_ADS_V2, SkipOpenChestTimeByAdsHandler::class.java)

        // iap shop
        helper.addRequestHandler(SFSCommand.GET_GEM_SHOP_V2, GetGemShopHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_PACK_SHOP_V2, GetPackShopHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_GOLD_SHOP_V2, GetGoldShopHandler::class.java)
        helper.addRequestHandler(SFSCommand.BUY_GOLD_V2, BuyGoldHandler::class.java)
        helper.addRequestHandler(SFSCommand.BUY_GEM_V2, UserBuyGemHandler::class.java)
        helper.addRequestHandler(SFSCommand.BUY_PACK_V2, UserBuyPackHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_FREE_REWARD_CONFIGS_V2, GetFreeRewardConfigsHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_FREE_GEMS_V2, GetFreeGemsHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_FREE_GOLDS_V2, GetFreeGoldsHandler::class.java)

        // skin
        helper.addRequestHandler(SFSCommand.GET_SKIN_INVENTORY_V2, GetSkinInventoryHandler::class.java)
        helper.addRequestHandler(SFSCommand.OPEN_SKIN_CHEST_V2, OpenSkinChestHandler::class.java)
        helper.addRequestHandler(SFSCommand.ACTIVE_SKIN_CHEST_V3, ActiveSkinChestHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_COSTUME_SHOP_V2, GetCostumeShopHandler::class.java)
        helper.addRequestHandler(SFSCommand.BUY_COSTUME_ITEM_V2, BuyCostumeItemHandler::class.java)

        // uncategorized
        helper.addRequestHandler(SFSCommand.SEND_MESSAGE_SLACK_V2, SendMessageSlackHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_NEWCOMER_GIFTS_V2, GetNewcomerGiftsHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_START_GAME_CONFIG_V2, GetStartGameConfigHandler::class.java)
//        helper.addRequestHandler(SFSCommand.GET_GAME_DATA, GetGameDataHandler::class.java)
        helper.addRequestHandler(SFSCommand.APPROVE_CLAIM_V2, ApproveClaimWithoutConfirmHandler::class.java)
        helper.addRequestHandler(SFSCommand.CONFIRM_CLAIM_REWARD_SUCCESS_V2, ConfirmClaimHandler::class.java)
//        helper.addRequestHandler(SFSCommand.USER_STAKE_V2, UserStakeHandler::class.java)
//        helper.addRequestHandler(SFSCommand.USER_WITHDRAW_STAKE_V2, UserWithdrawStakeHandler::class.java)
//        helper.addRequestHandler(SFSCommand.USER_RENAME_V2, UserRenameHandler::class.java)
        helper.addRequestHandler(SFSCommand.USER_LINK_V2, UserLinkHandler::class.java)
        helper.addRequestHandler(SFSCommand.DELETE_USER_V2, DeleteUserHandler::class.java)
        helper.addRequestHandler(SFSCommand.CHECK_NO_ADS_V2, CheckNoAdsHandler::class.java)
//        helper.addRequestHandler(SFSCommand.GET_EMAIL_V2, GetEmailHandler::class.java)
//        helper.addRequestHandler(SFSCommand.REGISTER_EMAIL_V2, RegisterEmailHandler::class.java)
//        helper.addRequestHandler(SFSCommand.VERIFY_EMAIL_V2, VerifyEmailHandler::class.java)
        helper.addRequestHandler(SFSCommand.SWAP_TOKEN_V2, ConvertTokenHandler::class.java)
        helper.addRequestHandler(SFSCommand.PREVIEW_TOKEN_V2, PreviewTokenHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_DAILY_MISSION_V2, GetDailyMissionHandler::class.java)
//        helper.addRequestHandler(SFSCommand.GET_QUESTS_V2, GetQuestsHandler::class.java)
//        helper.addRequestHandler(SFSCommand.GET_ACHIEVEMENT_V2, GetAchievementHandler::class.java)
        helper.addRequestHandler(SFSCommand.WATCHING_DAILY_MISSION_ADS_V2, WatchingDailyAdsHandler::class.java)
        helper.addRequestHandler(SFSCommand.TAKE_DAILY_MISSION_REWARD_V2, TakeMissionRewardHandler::class.java)
//        helper.addRequestHandler(SFSCommand.CU_COSTUME_PRESET_V2, CreateOrUpdateCostumePresetHandler::class.java)
//        helper.addRequestHandler(SFSCommand.BUY_COSTUME_PRESET_SLOT_V2, BuyCostumePresetSlotHandler::class.java)
//        helper.addRequestHandler(SFSCommand.GET_COSTUME_PRESET_V2, GetCostumePresetHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_LUCKY_WHEEL_REWARD_V2, GetLuckyWheelReward::class.java)
//        helper.addRequestHandler(SFSCommand.GET_OFFLINE_REWARDS_V2, GetOfflineRewardsHandler::class.java)
//        helper.addRequestHandler(SFSCommand.CLAIM_OFFLINE_REWARDS_V2, ClaimOfflineRewardsHandler::class.java)
        helper.addRequestHandler(SFSCommand.MARK_ITEM_VIEWED_V2, MarkItemViewedHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_SWAP_TOKEN_CONFIG_V2, GetSwapTokenConfig::class.java)
        helper.addRequestHandler(SFSCommand.GET_SERVER_INFO_V2, GetServerInfoHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_OTHER_USER_INFO_V2, GetOtherUserInfoHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_DAILY_TASK_CONFIG, GetDailyTaskConfigHandler::class.java)
        helper.addRequestHandler(SFSCommand.CLAIM_DAILY_TASK, ClaimDailyTaskHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_USER_DAILY_PROGRESS, GetUserDailyProgressHandler::class.java)

        // new handler not wait api response
        helper.addRequestHandler(SFSCommand.SYNC_HOUSE_V3, SyncHouseV3Handler::class.java)
    }

    override fun initStreamListeners() {
        val messenger = _services.get<IMessengerService>()
        val usersManagers = _services.get<ISvServicesContainer>().filter(IUsersManager::class)

        val adminCmdStreamProcessor = AdminCommandStreamProcessor(_zone, _extension, _services)

        messenger.listen(StreamKeys.SV_KICK_USER) { message ->
            usersManagers.forEach {
                it.kickAndRemoveUser(message.value)
            }
            false
        }

        messenger.listen(StreamKeys.SV_ADMIN_COMMAND) { message ->
            adminCmdStreamProcessor.process(message.value)
            false
        }
    }

    override fun initSchedulers() {
        ExtensionSchedulerAll(_services).initialize()
    }
}
