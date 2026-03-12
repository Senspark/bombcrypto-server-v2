package com.senspark.game.db

import com.senspark.common.service.IGlobalService
import com.senspark.game.constant.Booster
import com.senspark.game.declare.EnumConstants

interface ILogDataAccess : IGlobalService {
    fun logClaimReward(userName: String, type: String, value: Float, apiStatus: String)
    fun logRefundReward(userName: String, type: String, value: Float, refundReason: String)
    fun logHack(userName: String, hackType: Int, data: String)

    fun logPlayPve(
        uid: Int,
        stage: Int,
        level: Int,
        result: EnumConstants.MatchResult,
        totalTime: Long,
        boosters: Map<Booster, Int>,
        heroId: Int
    )
}