package com.senspark.game.db.model

interface IUserConfig {
    val uid: Int
    val userGachaChestSlots: List<UserGachaChestSlot>
}