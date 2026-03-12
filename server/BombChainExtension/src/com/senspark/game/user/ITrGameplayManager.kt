package com.senspark.game.user

import com.senspark.common.service.IGlobalService
import com.senspark.game.declare.EnumConstants

interface ITrGameplayManager : IGlobalService {
    fun joinAdventure(uid: Int, network: EnumConstants.DataType)
    fun leaveAdventure(uid: Int, network: EnumConstants.DataType)
    fun isPlayingAdventure(uid: Int): Boolean
    fun resetAdventure(uid: Int)

    fun joinPvp(uid: Int, network: EnumConstants.DataType)
    fun leavePvp(uid: Int, network: EnumConstants.DataType)
    fun isPlayingPvp(uid: Int): Boolean
    fun resetPvp(uid: Int)

    fun getCurrentTypePlayingPvp(uid: Int): EnumConstants.DataType?

    fun isPlaying(uid: Int): Boolean

    fun leaveAll(uid: Int, network: EnumConstants.DataType)
    fun resetAll(uid: Int)
}