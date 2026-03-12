package com.senspark.game.pvp.manager

import com.senspark.common.pvp.IMatchData
import com.senspark.common.pvp.IMoveHeroData
import com.senspark.common.pvp.IPlantBombData
import com.senspark.common.utils.ILogger
import com.senspark.game.constant.Booster
import com.senspark.game.pvp.command.*
import com.senspark.game.pvp.data.IMatch
import com.senspark.game.pvp.data.IMatchObserveData
import kotlin.coroutines.suspendCoroutine

class DefaultCommandManager(
    private val _logger: ILogger,
    private val _match: IMatch,
    private val _matchData: IMatchData,
    private val _packetManager: IPacketManager,
    private val _stateManager: IStateManager,
    private val _dataFactory: IObserveDataFactory,
) : ICommandManager {
    private val _commands = mutableListOf<ICommand>()
    private val _commandsLocker = Any()

    override suspend fun moveHero(slot: Int, timestamp: Int, x: Float, y: Float): IMoveHeroData {
        return suspendCoroutine {
            synchronized(_commandsLocker) {
                _commands.add(
                    MoveHeroCommand(
                        _logger,
                        _packetManager,
                        timestamp,
                        _match,
                        it,
                        slot,
                        x,
                        y,
                    )
                )
            }
        }
    }

    override suspend fun plantBomb(slot: Int, timestamp: Int): IPlantBombData {
        return suspendCoroutine {
            synchronized(_commandsLocker) {
                _commands.add(
                    PlantBombCommand(
                        _logger,
                        _packetManager,
                        timestamp,
                        _match,
                        it,
                        slot,
                    )
                )
            }
        }
    }

    override suspend fun throwBomb(slot: Int, timestamp: Int) {
        return suspendCoroutine {
            synchronized(_commandsLocker) {
                _commands.add(
                    ThrowBombCommand(
                        _logger,
                        _packetManager,
                        timestamp,
                        _match,
                        it,
                        slot,
                    )
                )
            }
        }
    }

    override suspend fun useBooster(slot: Int, timestamp: Int, item: Booster) {
        return suspendCoroutine {
            synchronized(_commandsLocker) {
                _commands.add(
                    UseBoosterCommand(
                        _logger,
                        _packetManager,
                        timestamp,
                        _match,
                        it,
                        slot,
                        item,
                    )
                )
            }
        }
    }

    override fun processCommands(): List<IMatchObserveData> {
        val commandsByTimestamp: Map<Int, List<ICommand>>
        synchronized(_commandsLocker) {
            commandsByTimestamp = _commands
                .sortedBy { it.timestamp }
                .groupBy { it.timestamp }
            _commands.clear()
        }
        val dataList = commandsByTimestamp.mapNotNull { (timestamp, commands) ->
            commands.forEach {
                it.handle()
            }
            val stateDelta = _stateManager.processState() ?: return@mapNotNull null
            _dataFactory.generate(timestamp + _matchData.roundStartTimestamp, stateDelta)
        }
        return dataList
    }
}