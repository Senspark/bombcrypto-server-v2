package com.senspark.game.pvp.manager

import com.smartfoxserver.v2.api.CreateRoomSettings
import com.smartfoxserver.v2.entities.Room

interface IRoomCreator {
    fun createRoom(settings: CreateRoomSettings): Room
}