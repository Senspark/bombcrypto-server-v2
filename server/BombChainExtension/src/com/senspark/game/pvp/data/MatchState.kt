package com.senspark.game.pvp.data

import com.senspark.game.pvp.delta.IMatchStateDelta
import com.senspark.game.pvp.manager.*

class MatchState(
    override val heroState: IHeroManagerState,
    override val bombState: IBombManagerState,
    override val mapState: IMapManagerState,
) : IMatchState {
    companion object {
        fun decodeDelta(delta: IMatchStateDelta): IMatchState {
            return MatchState(
                heroState = HeroManagerState.decodeDelta(delta.hero),
                bombState = BombManagerState.decodeDelta(delta.bomb),
                mapState = MapManagerState.decodeDelta(delta.block),
            )
        }
    }

    override fun apply(state: IMatchState): IMatchState {
        return MatchState(
            heroState = heroState.apply(state.heroState),
            bombState = bombState.apply(state.bombState),
            mapState = mapState.apply(state.mapState),
        )
    }
}