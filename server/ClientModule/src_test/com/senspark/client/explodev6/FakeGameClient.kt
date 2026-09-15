package com.senspark.client.explodev6

import com.senspark.game.declare.ErrorCode
import com.senspark.game.pvp.HandlerCommand
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

// Unity client's wire format from DefaultPveServerBridge.cs (field types copied exactly,
// e.g. id as long), plus [fireFuse] to trigger the server-timed explode.
class FakeGameClient(private val bed: ServerTestBed) {

    data class BombTarget(val heroId: Int, val heroType: Int, val location: Cell)

    fun getBombTargets(
        seeds: Map<Int, Cell>,
        rejects: Map<Int, Cell> = emptyMap(),
    ): List<BombTarget> {
        val heroesArr: ISFSArray = SFSArray()
        for ((heroId, seed) in seeds) {
            val entry: ISFSObject = SFSObject()
            entry.putLong("id", heroId.toLong())
            rejects[heroId]?.let {
                entry.putInt("reject_i", it.i)
                entry.putInt("reject_j", it.j)
            }
            entry.putInt("i", seed.i)
            entry.putInt("j", seed.j)
            heroesArr.addSFSObject(entry)
        }
        val data: ISFSObject = SFSObject()
        data.putSFSArray("heroes", heroesArr)

        val response = bed.dispatch(bed.getBombTargetHandler, data)
        check(!response.isError) { "GET_BOMB_TARGET failed with error ${response.errorCode}" }
        return readTargets(response.require().getSFSArray("targets"))
    }

    fun getBombTarget(heroId: Int, seed: Cell, reject: Cell? = null): Cell? {
        val rejects = if (reject == null) emptyMap() else mapOf(heroId to reject)
        return getBombTargets(mapOf(heroId to seed), rejects).firstOrNull { it.heroId == heroId }?.location
    }

    fun startPlantBomb(heroId: Int, bombNo: Int, cell: Cell): PlantResult {
        val data: ISFSObject = SFSObject()
        data.putLong("id", heroId.toLong())
        data.putInt("num", bombNo)
        data.putInt("i", cell.i)
        data.putInt("j", cell.j)

        val response = bed.dispatch(bed.startPlantBombHandler, data)
        if (response.isError) {
            return PlantResult(response.errorCode, null)
        }
        val body = response.require()
        return PlantResult(
            null,
            BombTarget(body.getLong("id").toInt(), body.getInt("hero_type"), cell(body.getInt("i"), body.getInt("j"))),
        )
    }

    data class PlantResult(val errorCode: Int?, val nextTarget: BombTarget?) {
        val isOk get() = errorCode == null
        val isTargetMismatch get() = errorCode == ErrorCode.PLANT_TARGET_MISMATCH
        val isTooFast get() = errorCode == ErrorCode.PLANT_TOO_FAST
        val isNoTarget get() = errorCode == ErrorCode.NO_BOMB_TARGET
        fun requireNextTarget(): Cell =
            nextTarget?.location ?: error("expected a next target, got error code $errorCode")
    }

    // Returns the RESPONSE_EXPLODE push from this fuse only; isPending=false if none was armed.
    fun fireFuse(heroId: Int, bombNo: Int, cell: Cell): ExplodeResult {
        val before = bed.pushes.size
        val wasPending = bed.fireFuse(heroId, bombNo, cell.i, cell.j)
        val push = bed.pushes.drop(before).lastOrNull { it.command == HandlerCommand.ResponseExplode }
        return ExplodeResult(wasPending, push?.data)
    }

    data class ExplodeResult(val isPending: Boolean, val data: ISFSObject?) {
        val pushed get() = data != null
        val energy get() = data?.getInt("energy")
        val bombNo get() = data?.getInt("num")
        val cell get() = data?.let { cell(it.getInt("i"), it.getInt("j")) }
        // (i, j, hp)
        val blocks: List<Triple<Int, Int, Int>>
            get() {
                val arr = data?.getSFSArray("blocks") ?: return emptyList()
                return (0 until arr.size()).map {
                    val o = arr.getSFSObject(it)
                    Triple(o.getInt("i"), o.getInt("j"), o.getInt("hp"))
                }
            }
    }

    private fun readTargets(arr: ISFSArray?): List<BombTarget> {
        if (arr == null) return emptyList()
        return (0 until arr.size()).map {
            val o = arr.getSFSObject(it)
            BombTarget(o.getLong("id").toInt(), o.getInt("hero_type"), cell(o.getInt("i"), o.getInt("j")))
        }
    }
}
