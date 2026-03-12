package com.senspark.game.data.manager.dailyMission

import com.senspark.game.declare.customEnum.MissionAction

interface IMissionProcessor {
    fun completeMission(uid: Int, actions: List<Pair<MissionAction, Int>>)
}