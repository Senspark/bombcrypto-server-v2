package com.senspark.game.manager.user

import com.senspark.common.service.IServerService

interface IUserLinkManager : IServerService {
    /**
     * Attempts to link two users.
     * @param userId Own user ID.
     * @param linkedUserId To be linked user ID.
     */
    fun link(userId: Int, linkedUserId: Int)

    /**
     * Gets the linked user ID list.
     */
    fun getLinkedUserId(userId: Int): List<Int>

    /**
     * Gets the linked to (parent) user ID.
     */
    fun getLinkedToUserId(userId: Int): Int
}