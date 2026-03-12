package com.senspark.game.pvp.command

import com.senspark.common.utils.ILogger
import com.senspark.game.constant.Booster
import com.senspark.game.pvp.data.IMatch
import com.senspark.game.pvp.manager.IPacketManager
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UseBoosterCommand(
    private val _logger: ILogger,
    private val _packetManager: IPacketManager,
    override val timestamp: Int,
    private val _match: IMatch,
    private val _cont: Continuation<Unit>,
    private val _slot: Int,
    private val _item: Booster,
) : ICommand {
    override fun handle() {
        try {
            val hero = _match.heroManager.getHero(_slot)
            hero.useBooster(_item)
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