package com.senspark.game.data.model.config

import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSObject
import java.sql.ResultSet

class UpgradeCrystal(
    val sourceItemId: Int,
    val targetItemId: Int,
    val gemFee: Int,
    val goldFee: Int
) {
    companion object {
        fun fromResultSet(rr: ResultSet): UpgradeCrystal {
            return UpgradeCrystal(
                rr.getInt("source_item_id"),
                rr.getInt("target_item_id"),
                rr.getInt("gem_fee"),
                rr.getInt("gold_fee"),
            )

        }
    }

    fun toSfsObject(): ISFSObject {
        return SFSObject().apply {
            putInt("source_item_id", sourceItemId)
            putInt("target_item_id", targetItemId)
            putInt("gem_fee", gemFee)
            putInt("gold_fee", goldFee)
        }
    }
}