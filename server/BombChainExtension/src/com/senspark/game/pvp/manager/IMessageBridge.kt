package com.senspark.game.pvp.manager

import com.senspark.game.api.IPvpResultInfo
import com.senspark.game.pvp.data.*
import com.smartfoxserver.v2.entities.User

interface IMessageBridge {
    fun ping(data: IPingPongData, users: List<User>)
    fun startReady(users: List<User>)
    fun ready(data: IMatchReadyData, users: List<User>)
    fun finishReady(users: List<User>)
    fun startRound(data: IMatchStartData, users: List<User>)
    fun useEmoji(data: IUseEmojiData, users: List<User>)
    fun bufferFallingBlocks(data: IFallingBlockData, users: List<User>)
    fun changeState(data: IMatchObserveData, users: List<User>)
    fun finishRound(data: IMatchFinishData, users: List<User>)
    fun finishMatch(data: IPvpResultInfo, users: List<User>)
}