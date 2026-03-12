package com.senspark.common.pvp

import com.smartfoxserver.v2.entities.User

interface IMatchController {
    val matchInfo: IMatchInfo
    val matchData: IMatchData
    val matchStats: IMatchStats

    fun initialize()

    fun joinRoom(user: User)
    fun leaveRoom(user: User)

    /**
     * Ready the specified user.
     * @param user Who made the request.
     */
    fun ready(user: User)

    /**
     * Quits the current match.
     * @param user Who made the request.
     */
    fun quit(user: User)

    /**
     * Handles a ping request.
     * @param user Who made the request.
     * @param timestamp Client timestamp.
     * @param requestId request ID.
     */
    fun ping(user: User, timestamp: Long, requestId: Int)

    /**
     * Handles a move command.
     * @param user Who made the request.
     * @param timestamp Client timestamp.
     * @param x Client horizontal position.
     * @param y Client vertical position.
     */
    suspend fun moveHero(user: User, timestamp: Long, x: Float, y: Float): IMoveHeroData

    /**
     * Handles a plant bomb command.
     * @param user Who made the request.
     * @param timestamp Client timestamp.
     */
    suspend fun plantBomb(user: User, timestamp: Long): IPlantBombData

    suspend fun throwBomb(user: User, timestamp: Long)

    /**
     * Handles a use booster command.
     * @param user Who made the request.
     * @param timestamp Client timestamp.
     * @param itemId Booster item ID.
     */
    suspend fun useBooster(user: User, timestamp: Long, itemId: Int)

    fun useEmoji(user: User, itemId: Int)
}