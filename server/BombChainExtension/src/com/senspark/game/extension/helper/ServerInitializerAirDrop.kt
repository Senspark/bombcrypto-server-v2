package com.senspark.game.extension.helper

import com.senspark.game.declare.SFSCommand
import com.senspark.game.handler.airdropUser.*

class ServerInitializerAirDrop : IServerInitializer {
    override fun initHandlers(helper: AddRequestHandlerHelper) {
        helper.addRequestHandler(SFSCommand.ADD_HERO_FOR_AIRDROP_USER, AddHeroAirdropHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_COIN_RANKING_V2, GetCoinRankingV2Handler::class.java)
        helper.addRequestHandler(
            SFSCommand.GET_ALL_SEASON_COIN_RANKING_V2,
            GetAllSeasonCoinRankingV2Handler::class.java
        )
        helper.addRequestHandler(SFSCommand.BUY_HOUSE_SERVER, BuyHouseServerHandler::class.java)
        helper.addRequestHandler(SFSCommand.BUY_HERO_SERVER, BuyHeroServerHandler::class.java)
        helper.addRequestHandler(SFSCommand.FUSION_HERO_SERVER, FusionHeroServerHandler::class.java)
        helper.addRequestHandler(SFSCommand.MULTI_FUSION_HERO_SERVER, MultiFusionHeroServerHandler::class.java)
        helper.addRequestHandler(SFSCommand.GET_RENT_HOUSE_PACKAGE_CONFIG, GetRentHousePackageConfigHandler::class.java)
        helper.addRequestHandler(SFSCommand.RENT_HOUSE, UserRentHouseHandler::class.java)
        helper.addRequestHandler(
            SFSCommand.GET_COIN_LEADERBOARD_CONFIG_V2,
            GetCoinLeaderboardConfigV2Handler::class.java
        )
    }

    override fun initStreamListeners() {
    }

    override fun initSchedulers() {
    }
}