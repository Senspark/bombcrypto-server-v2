package com.senspark.game.pvp.manager

interface IPacketManager {
    fun add(action: () -> Unit)
    fun flush()
}