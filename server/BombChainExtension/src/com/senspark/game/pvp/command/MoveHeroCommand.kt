package com.senspark.game.pvp.command

import com.senspark.common.pvp.IMoveHeroData
import com.senspark.common.utils.ILogger
import com.senspark.game.pvp.data.IMatch
import com.senspark.game.pvp.entity.BlockReason
import com.senspark.game.pvp.entity.isItem
import com.senspark.game.pvp.manager.IPacketManager
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

class MoveHeroCommand(
    private val _logger: ILogger,
    private val _packetManager: IPacketManager,
    override val timestamp: Int,
    private val _match: IMatch,
    private val _cont: Continuation<IMoveHeroData>,
    private val _slot: Int,
    private val _x: Float,
    private val _y: Float,
) : ICommand {
    override fun handle() {
        try {
            val hero = _match.heroManager.getHero(_slot)
            verify(hero.x, hero.y, _x, _y)
            hero.move(timestamp, _x, _y)
            val xInt = hero.x.toInt()
            val yInt = hero.y.toInt()
            val block = _match.mapManager.getBlock(xInt, yInt)
            if (block != null && block.isItem) {
                block.kill(BlockReason.Consumed)
                hero.takeItem(block.type)
            }
            val x = hero.x
            val y = hero.y
            _packetManager.add {
                _cont.resume(object : IMoveHeroData {
                    override val x = x
                    override val y = y
                })
            }
        } catch (ex: Exception) {
            _packetManager.add {
                _cont.resumeWithException(ex)
            }
        }
    }

    private fun verify(
        oldX: Float,
        oldY: Float,
        newX: Float,
        newY: Float,
    ) {
        val intX = newX.toInt()
        val intY = newY.toInt()
        val block = _match.mapManager.getBlock(intX, intY)
        if (block != null) {
            require(block.isItem) { "Cannot move to [$newX, $newY]: block exists [$intX, $intY] type=${block.type}" }
        }

        val oldIntX = oldX.toInt()
        val oldIntY = oldY.toInt()
        val distance = abs(intX - oldIntX) + abs(intY - oldIntY)
        require(distance <= 1) { "Move distance larger than 1" }
    }
}