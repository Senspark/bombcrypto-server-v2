package com.senspark.game.pvp.manager

import com.senspark.common.pvp.IMatchHeroInfo
import com.senspark.common.pvp.IMatchTeamInfo
import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.config.IHeroConfig
import com.senspark.game.pvp.delta.IHeroStateDelta
import com.senspark.game.pvp.entity.*
import com.senspark.game.pvp.user.IParticipantController
import com.senspark.game.pvp.utility.IRandom
import com.senspark.game.pvp.utility.LongBitDecoder

class HeroManagerState(
    override val heroes: Map<Int, IHeroState>,
) : IHeroManagerState {
    companion object {
        fun decodeDelta(delta: List<IHeroStateDelta>): IHeroManagerState {
            return HeroManagerState(heroes = delta.associate {
                val baseState = it.base
                val positionState = it.position
                it.slot to HeroState(
                    if (baseState == null) null else HeroBaseState.decode(baseState.state),
                    if (positionState == null) null else HeroPositionState.decode(positionState.state),
                )
            })
        }

        fun decode(state: List<Long>): IHeroManagerState {
            val heroes = mutableMapOf<Int, IHeroState>()
            var index = 0
            while (index < state.size) {
                val decoder = LongBitDecoder(state[index])
                val size = decoder.popInt(5)
                val slot = decoder.popInt(3)
                val heroState = HeroState(
                    HeroBaseState.decode(state.subList(index + 2, index + size)),
                    HeroPositionState.decode(state[index + 1]),
                )
                heroes[slot] = heroState
                index += size
            }
            return HeroManagerState(heroes)
        }
    }

    override fun apply(state: IHeroManagerState): IHeroManagerState {
        val items = heroes.toMutableMap()
        state.heroes.forEach { (key, value) ->
            val item = items[key] ?: throw IllegalArgumentException("Invalid hero key=$key")
            items[key] = HeroState(
                baseState = value.baseState ?: item.baseState,
                positionState = value.positionState ?: item.positionState,
            )
        }
        return HeroManagerState(
            heroes = items,
        )
    }

    override fun encode(): List<Long> {
        // FIXME: not used.
//        return heroes.map { (slot, state) ->
//            val baseState = listOf(state.positionState.encode(), *state.baseState.encode().toTypedArray())
//            val encoder = LongBitEncoder()
//                .push(baseState.size + 1, 5) // +1 for base encoder.
//                .push(slot, 3)
//            listOf(encoder.value, *baseState.toTypedArray())
//        }.flatten()
        return emptyList()
    }
}

class DefaultHeroManager(
    controllers: List<IParticipantController>,
    teamInfo: List<IMatchTeamInfo>,
    heroInfo: List<IMatchHeroInfo>,
    initialState: IHeroManagerState,
    heroConfig: IHeroConfig,
    private val _logger: ILogger,
    bombManager: IBombManager,
    timeManager: ITimeManager,
    random: IRandom,
    listener: IHeroListener,
) : IHeroManager {
    private val _heroes = initialState.heroes.mapValues { (slot, state) ->
        val teamId = teamInfo.indexOfFirst { it.slots.contains(slot) }
        Hero(
            slot = slot,
            teamId = teamId,
            initialState = state,
            _controller = controllers[slot],
            _config = heroConfig,
            _info = heroInfo[slot],
            _logger = _logger,
            _bombManager = bombManager,
            _timeManager = timeManager,
            _random = random,
            _listener = object : IHeroListener {
                override fun onDamaged(hero: IHero, amount: Int, source: HeroDamageSource) {
                    listener.onDamaged(hero, amount, source)
                }

                override fun onHealthChanged(hero: IHero, amount: Int, oldAmount: Int) {
                    listener.onHealthChanged(hero, amount, oldAmount)
                }

                override fun onItemChanged(hero: IHero, item: HeroItem, amount: Int, oldAmount: Int) {
                    listener.onItemChanged(hero, item, amount, oldAmount)
                }

                override fun onEffectBegan(hero: IHero, effect: HeroEffect, reason: HeroEffectReason, duration: Int) {
                    listener.onEffectBegan(hero, effect, reason, duration)
                }

                override fun onEffectEnded(hero: IHero, effect: HeroEffect, reason: HeroEffectReason) {
                    listener.onEffectEnded(hero, effect, reason)
                }

                override fun onMoved(hero: IHero, x: Float, y: Float) {
                    listener.onMoved(hero, x, y)
                    onHeroMoved(hero, x, y)
                }
            }
        )
    }

    override val state: IHeroManagerState
        get() {
            val states = _heroes.mapValues { it.value.state }
            return HeroManagerState(states)
        }

    override fun applyState(state: IHeroManagerState) {
        state.heroes.forEach { (slot, heroState) ->
            val hero = _heroes[slot] ?: throw IllegalArgumentException("Invalid hero slot")
            hero.applyState(heroState)
        }
    }

    override fun getHero(slot: Int): IHero {
        return _heroes[slot] ?: throw IllegalArgumentException("Invalid hero slot")
    }

    override fun damageBomb(x: Int, y: Int, amount: Int) {
        _heroes.forEach { (_, item) ->
            if (item.x.toInt() == x &&
                item.y.toInt() == y) {
                item.damageBomb(amount)
            }
        }
    }

    override fun damageFallingBlock(x: Int, y: Int) {
        _heroes.forEach { (_, item) ->
            if (item.x.toInt() == x &&
                item.y.toInt() == y) {
                item.damageFallingBlock()
            }
        }
    }

    private fun onHeroMoved(hero: IHero, x: Float, y: Float) {
        _heroes.forEach { (_, item) ->
            if (item.x.toInt() == x.toInt() &&
                item.y.toInt() == y.toInt()) {
                if (item.teamId == hero.teamId) {
                    item.rescuePrison()
                } else {
                    item.damagePrison()
                }
            }
        }
    }

    override fun step(delta: Int) {
        _heroes.forEach { (_, hero) ->
            hero.update(delta)
        }
    }
}