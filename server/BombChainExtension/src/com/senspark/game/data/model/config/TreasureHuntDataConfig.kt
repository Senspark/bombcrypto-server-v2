package com.senspark.game.data.model.config

import com.senspark.game.declare.EnumConstants.DataType
import com.smartfoxserver.v2.entities.data.ISFSArray
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject

class TreasureHuntDataConfig(
    val dataConfigs: Map<DataType, ISFSObject>
) {
    companion object {
        private fun createDataConfig(result: ISFSArray): ISFSObject {
            val size = result.size()
            val data = SFSObject()
            for (i in 0 until size) {
                val rs = result.getSFSObject(i)
                val key = rs.getUtfString("key")
                val value = rs.getUtfString("value")
                data.putUtfString(key, value)
            }
            return data
        }

        fun fromResultSet(result: ISFSArray): TreasureHuntDataConfig {
            val dataConfig = mutableMapOf(
                DataType.BSC to SFSArray(),
                DataType.POLYGON to SFSArray(),
                DataType.TON to SFSArray(),
                DataType.SOL to SFSArray(),
                DataType.RON to SFSArray(),
                DataType.VIC to SFSArray(),
                DataType.BAS to SFSArray()
            )
            for (i in 0 until result.size()) {
                try {
                    val rs = result.getSFSObject(i)
                    val network = DataType.valueOf(rs.getUtfString("network"))
                    dataConfig[network]!!.addSFSObject(rs)
                } catch (e: Exception) {
                    // ignore
                }
            }

            return TreasureHuntDataConfig(
                dataConfig.map { it.key to createDataConfig(it.value) }.toMap()
            )
        }
    }
}