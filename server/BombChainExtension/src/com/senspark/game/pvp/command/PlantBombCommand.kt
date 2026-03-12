package com.senspark.game.pvp.command

import com.senspark.common.pvp.IPlantBombData
import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.data.IMatch
import com.senspark.game.pvp.manager.IPacketManager
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PlantBombCommand(
    private val _logger: ILogger,
    private val _packetManager: IPacketManager,
    override val timestamp: Int,
    private val _match: IMatch,
    private val _cont: Continuation<IPlantBombData>,
    private val _slot: Int,
) : ICommand {
    override fun handle() {
        try {
            val hero = _match.heroManager.getHero(_slot)
            val bomb = hero.plantBomb(timestamp, true)
            val id = bomb.id
            val x = bomb.x.toInt()
            val y = bomb.y.toInt()
            val plantTimestamp = bomb.plantTimestamp
            _packetManager.add {
                _cont.resume(object : IPlantBombData {
                    override val id = id
                    override val x = x
                    override val y = y
                    override val plantTimestamp = plantTimestamp
                })
            }
        } catch (ex: Exception) {
            _packetManager.add {
                _cont.resumeWithException(ex)
            }
        }
    }
}
