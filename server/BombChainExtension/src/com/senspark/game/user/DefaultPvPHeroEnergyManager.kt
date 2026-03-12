package com.senspark.game.user

import com.google.gson.Gson
import com.senspark.game.data.PvPHeroEnergyData
import com.senspark.common.utils.ILogger
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.SFSArray
import java.time.Instant

class DefaultPvPHeroEnergyManager(
    private val _logger: ILogger
) : IPvPHeroEnergyManager {
    private var _itemList = mutableListOf<PvPHeroEnergy>()
    private var _itemMap = mutableMapOf<Long, PvPHeroEnergy>()

    override fun getItem(id: Long): PvPHeroEnergy {
        _logger.log("[Pvp][DefaultPvPHeroEnergyManager:getItem] $id")
        var item = _itemMap[id]
        if (item == null) {
            item = PvPHeroEnergy(PvPHeroEnergyData(id.toInt(), 0, Instant.EPOCH), _logger)
            _itemList.add(item)
            _itemMap[id] = item
        }
        return item
    }

    override fun setItems(items: List<PvPHeroEnergy>) {
        _itemList = items.toMutableList()
        _itemMap = items.associateBy { it.heroId.toLong() }.toMutableMap()
    }

    override fun toJson(): String {
        val gson = Gson()
        val json = gson.toJson(_itemList.map { it.toJson() })
        return json
    }

    override fun toSFSArray(): ISFSArray {
        val result = SFSArray()
        _itemList.forEach {
            result.addSFSObject(it.toSFSObject())
        }
        return result
    }
}