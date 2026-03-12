package com.senspark.game.data.model.config

import com.smartfoxserver.v2.entities.data.SFSObject

class AirDrop(
    val code: String,
    val name: String,
    val idBlockChain: Int,
    private val rewardTotal: Int,
    private val rewardClaimed: Int,
    val rewardAmount: Int,
    val bomberToBuy: Int,
    val openDate: Long,
    val closeDate: Long
) {
    fun toSfsObject(): SFSObject {
        val sfsObject = SFSObject()
        sfsObject.putUtfString("code", code)
        sfsObject.putUtfString("name", name)
        sfsObject.putInt("rewardTotal", rewardTotal)
        sfsObject.putInt("rewardClaimed", rewardClaimed)
        sfsObject.putInt("rewardAmount", rewardAmount)
        sfsObject.putInt("bomberToBuy", bomberToBuy)
        sfsObject.putLong("openDate", openDate)
        sfsObject.putLong("closeDate", closeDate)

        return sfsObject
    }
}