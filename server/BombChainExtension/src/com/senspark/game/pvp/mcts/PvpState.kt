package com.senspark.game.pvp.mcts

import com.senspark.common.pvp.IMatchHeroInfo
import com.senspark.common.pvp.IMatchTeamInfo
import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.config.IHeroConfig
import com.senspark.game.pvp.data.IMatchState
import com.senspark.game.pvp.data.Match
import com.senspark.game.pvp.info.IMapInfo
import com.senspark.game.pvp.manager.ITimeManager
import com.senspark.game.pvp.user.IParticipantController
import com.senspark.game.pvp.utility.IRandom

class PvpState(
    controllers: List<IParticipantController>,
    teamInfo: List<IMatchTeamInfo>,
    heroInfo: List<IMatchHeroInfo>,
    mapInfo: IMapInfo,
    heroConfig: IHeroConfig,
    private val _matchState: IMatchState,
    private val _logger: ILogger,
    private val _timeManager: ITimeManager,
    private val _random: IRandom,
) : IState {
    private val _match = Match(
        controllers,
        teamInfo,
        heroInfo,
        mapInfo,
        heroConfig,
        _matchState,
        _logger,
        _timeManager,
        _random,
        null,
        null,
        null,
    )

    override val lastAction: IAction
        get() = TODO("Not yet implemented")
    override val isTerminal: Boolean
        get() = TODO("Not yet implemented")
    override val lastPlayerIndex: Int
        get() = TODO("Not yet implemented")
    override val nextPlayerIndex: Int
        get() = TODO("Not yet implemented")
    override val actions: List<IAction>
        get() = TODO("Not yet implemented")

    override fun applyAction(action: IAction) {
        TODO("Not yet implemented")
    }

    override fun applyRandomAction() {
        TODO("Not yet implemented")
    }

    override fun getReward(playerIndex: Int): Float {
        TODO("Not yet implemented")
    }

    override fun clone(): IState {
        TODO("Not yet implemented")
    }
}