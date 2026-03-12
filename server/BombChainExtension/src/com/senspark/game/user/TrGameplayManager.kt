package com.senspark.game.user

import com.senspark.game.declare.EnumConstants

class TrGameplayManager : ITrGameplayManager {
    private val adventurePlayers = mutableMapOf<Int, EnumConstants.DataType>()
    private val pvpPlayers = mutableMapOf<Int, EnumConstants.DataType>()

    override fun initialize() {
    }

    override fun joinAdventure(uid: Int, network: EnumConstants.DataType) {
        adventurePlayers[uid] = network
    }

    override fun leaveAdventure(uid: Int, network: EnumConstants.DataType) {
        // Only remove if the current value matches the given network
        if (adventurePlayers[uid] == network) {
            adventurePlayers.remove(uid)
        }
    }

    /**
     * Dùng để check mỗi khi user muốn chơi adventure mode để đảm bảo 1 user chỉ đc chơi ở 1 network
     */
    override fun isPlayingAdventure(uid: Int): Boolean {
        // can try remove if check that uid with network is logout
        return adventurePlayers.containsKey(uid)
    }

    override fun resetAdventure(uid: Int) {
        adventurePlayers.remove(uid)
    }

    override fun joinPvp(uid: Int, network: EnumConstants.DataType) {
        pvpPlayers[uid] = network
    }

    override fun leavePvp(uid: Int, network: EnumConstants.DataType) {
        // Only remove if the current value matches the given network
        if (pvpPlayers[uid] == network) {
            pvpPlayers.remove(uid)
        }
    }

    override fun isPlayingPvp(uid: Int): Boolean {
        // can try remove if check that uid with network is logout
        return pvpPlayers.containsKey(uid)
    }

    override fun resetPvp(uid: Int) {
        pvpPlayers.remove(uid)
    }

    override fun getCurrentTypePlayingPvp(uid: Int): EnumConstants.DataType? {
        return pvpPlayers[uid]?.let { EnumConstants.DataType.valueOf(it.name) }
    }

    override fun isPlaying(uid: Int): Boolean {
        return isPlayingAdventure(uid) || isPlayingPvp(uid)
    }

    override fun leaveAll(uid: Int, network: EnumConstants.DataType) {
        leavePvp(uid, network)
        leaveAdventure(uid, network)
    }

    override fun resetAll(uid: Int) {
        resetPvp(uid)
        resetAdventure(uid)
    }
}