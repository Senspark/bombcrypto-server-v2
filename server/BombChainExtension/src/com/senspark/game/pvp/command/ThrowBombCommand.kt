package com.senspark.game.pvp.command

import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.data.IMatch
import com.senspark.game.pvp.manager.IPacketManager
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ThrowBombCommand(
    private val _logger: ILogger,
    private val _packetManager: IPacketManager,
    override val timestamp: Int,
    private val _match: IMatch,
    private val _cont: Continuation<Unit>,
    private val _slot: Int,
) : ICommand {
    override fun handle() {
        try {
            val hero = _match.heroManager.getHero(_slot)
            val xInt = hero.x.toInt()
            val yInt = hero.y.toInt()
            val bomb = _match.bombManager.getBomb(xInt, yInt)
                ?: throw Exception("No bomb to throw at [$xInt, $yInt]")
            _match.bombManager.throwBomb(bomb, hero.direction, 3, 500)
            _packetManager.add {
                _cont.resume(Unit)
            }
        } catch (ex: Exception) {
            _packetManager.add {
                _cont.resumeWithException(ex)
            }
        }
    }
}