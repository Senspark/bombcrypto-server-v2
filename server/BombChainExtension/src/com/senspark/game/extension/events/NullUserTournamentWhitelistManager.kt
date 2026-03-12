package com.senspark.game.extension.events

class NullUserTournamentWhitelistManager : IUserTournamentWhitelistManager {
    override fun initialize() {
    }
    
    override fun isInWhitelist(userId: String): Boolean {
        return false
    }

    override fun destroy() {}
}