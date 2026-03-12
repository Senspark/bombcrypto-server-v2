package com.senspark.game.manager.user

class NullUserLinkManager : IUserLinkManager {

    override fun initialize() {
    }
    
    override fun link(userId: Int, linkedUserId: Int) {}

    override fun getLinkedUserId(userId: Int): List<Int> {
        return emptyList()
    }

    override fun getLinkedToUserId(userId: Int): Int {
        return 0
    }
}