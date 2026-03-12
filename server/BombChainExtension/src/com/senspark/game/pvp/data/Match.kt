package com.senspark.game.pvp.data

import com.senspark.common.pvp.IMatchHeroInfo
import com.senspark.common.pvp.IMatchTeamInfo
import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.config.IHeroConfig
import com.senspark.game.pvp.entity.*
import com.senspark.game.pvp.info.IMapInfo
import com.senspark.game.pvp.manager.*
import com.senspark.game.pvp.user.IParticipantController
import com.senspark.game.pvp.utility.IRandom

class Match(
    controllers: List<IParticipantController>,
    teamInfo: List<IMatchTeamInfo>,
    heroInfo: List<IMatchHeroInfo>,
    mapInfo: IMapInfo,
    heroConfig: IHeroConfig,
    initialState: IMatchState,
    private val _logger: ILogger,
    private val _timeManager: ITimeManager,
    random: IRandom,
    heroListener: IHeroListener?,
    bombListener: IBombListener?,
    mapListener: IMapListener?,
) : IMatch {
    override val state: IMatchState
        get() = MatchState(
            heroManager.state,
            bombManager.state,
            mapManager.state,
        )

    override val heroManager: IHeroManager
    override val bombManager: IBombManager
    override val mapManager: IMapManager

    init {
        mapManager = DefaultMapManager.createMap(
            info = mapInfo,
            logger = _logger,
            timeManager = _timeManager,
            random = random,
            listener = object : IMapListener {
                override fun onAdded(block: IBlock, reason: BlockReason) {
                    mapListener?.onAdded(block, reason)
                }

                override fun onRemoved(block: IBlock, reason: BlockReason) {
                    mapListener?.onRemoved(block, reason)
                }
            }
        )
        bombManager = DefaultBombManager(
            initialState = initialState.bombState,
            _logger = _logger,
            _mapManager = mapManager,
            _timeManager = _timeManager,
            _listener = object : IBombListener {
                override fun onAdded(bomb: IBomb, reason: BombReason) {
                    bombListener?.onAdded(bomb, reason)
                }

                override fun onRemoved(bomb: IBomb, reason: BombReason) {
                    bombListener?.onRemoved(bomb, reason)
                }

                override fun onExploded(bomb: IBomb, ranges: Map<Direction, Int>) {
                    bombListener?.onExploded(bomb, ranges)
                }

                override fun onDamaged(x: Int, y: Int, amount: Int) {
                    bombListener?.onDamaged(x, y, amount)
                    this@Match.onBombDamaged(x, y, amount)
                }
            },
        )
        heroManager = DefaultHeroManager(
            controllers = controllers,
            teamInfo = teamInfo,
            heroInfo = heroInfo,
            initialState = initialState.heroState,
            heroConfig = heroConfig,
            _logger = _logger,
            bombManager = bombManager,
            timeManager = _timeManager,
            random = random,
            listener = object : IHeroListener {
                override fun onDamaged(hero: IHero, amount: Int, source: HeroDamageSource) {
                    heroListener?.onDamaged(hero, amount, source)
                }

                override fun onHealthChanged(hero: IHero, amount: Int, oldAmount: Int) {
                    heroListener?.onHealthChanged(hero, amount, oldAmount)
                }

                override fun onItemChanged(hero: IHero, item: HeroItem, amount: Int, oldAmount: Int) {
                    heroListener?.onItemChanged(hero, item, amount, oldAmount)
                }

                override fun onEffectBegan(hero: IHero, effect: HeroEffect, reason: HeroEffectReason, duration: Int) {
                    heroListener?.onEffectBegan(hero, effect, reason, duration)
                }

                override fun onEffectEnded(hero: IHero, effect: HeroEffect, reason: HeroEffectReason) {
                    heroListener?.onEffectEnded(hero, effect, reason)
                }

                override fun onMoved(hero: IHero, x: Float, y: Float) {
                    heroListener?.onMoved(hero, x, y)
                }
            }
        )
    }

    override fun applyState(state: IMatchState) {
        heroManager.applyState(state.heroState)
        bombManager.applyState(state.bombState)
        mapManager.applyState(state.mapState)
    }

    private fun onBombDamaged(x: Int, y: Int, amount: Int) {
        heroManager.damageBomb(x, y, amount)
    }

    override fun step(delta: Int) {
        heroManager.step(delta)
        bombManager.step(delta)
    }
}