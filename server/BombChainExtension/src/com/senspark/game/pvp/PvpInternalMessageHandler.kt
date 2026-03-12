package com.senspark.game.pvp

import com.senspark.common.constant.PVPInternalCommand
import com.senspark.common.pvp.IRoomExtension
import com.senspark.common.utils.ILogger
import com.senspark.game.api.IInternalMessageHandler
import com.senspark.game.pvp.utility.JsonUtility
import com.smartfoxserver.v2.entities.Zone
import com.smartfoxserver.v2.entities.data.ISFSObject
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.encodeToString

class PvpInternalMessageHandler(
    private val _logger: ILogger,
    private val _zone: Zone,
) : IInternalMessageHandler {
    override fun handle(command: String, params: ISFSObject): ISFSObject? {
        return when (command) {
            PVPInternalCommand.GET_PVP_ROOM_INFO -> handleRoomInfo(params)
            else -> null
        }
    }

    override fun handle(command: String, params: String): ISFSObject? {
        TODO("Not yet implemented")
    }

    private fun handleRoomInfo(params: ISFSObject): ISFSObject {
        val rooms = _zone.roomList
        val json = JsonUtility.json
        val response = SFSObject().apply {
            putSFSArray("rooms", SFSArray().apply {
                rooms.forEach {
                    val extension = it.extension as IRoomExtension
                    addSFSObject(SFSObject().apply {
                        val controller = extension.controller
                        val matchInfo = controller.matchInfo
                        putSFSObject(
                            "match_info",
                            SFSObject.newFromJsonData(json.encodeToString(matchInfo)).apply {
                                putUtfString("hash", matchInfo.hash)
                            }
                        )
                        putSFSObject(
                            "match_data",
                            SFSObject.newFromJsonData(json.encodeToString(controller.matchData))
                        )
                        putSFSObject(
                            "match_stats",
                            SFSObject.newFromJsonData(json.encodeToString(controller.matchStats))
                        )
                    })
                }
            })
        }
        return response
    }
}