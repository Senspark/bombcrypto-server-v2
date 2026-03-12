package com.senspark.game.pvp.command

interface ICommand {
    val timestamp: Int

    /**
     * Handles this command with the specified hero.
     */
    fun handle()
}