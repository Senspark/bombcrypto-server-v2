package com.senspark.game.manager.pvp

import com.senspark.common.cache.IMessengerService
import com.senspark.common.pvp.IRoomExtension
import com.senspark.game.constant.StreamKeys
import com.senspark.game.pvp.utility.JsonUtility
import com.smartfoxserver.v2.entities.Zone
import com.smartfoxserver.v2.entities.data.SFSArray
import com.smartfoxserver.v2.entities.data.SFSObject
import kotlinx.serialization.encodeToString

/**
 * FIXME: Làm nhanh để kịp tiến độ:
 * - Mỗi 1 đơn vị thời gian sẽ cập nhật lại tình trạng của các rooms
 */
class MatchInfoUpdatedBroadcaster(
    private val _messenger: IMessengerService,
    private val _zone: Zone
) {
    private var _prevMessage = ""
    
    fun broadcast() {
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
        val message = response.toJson()
        if (message == _prevMessage) {
            return
        }
        _prevMessage = message
        _messenger.send(StreamKeys.SV_PVP_MATCH_UPDATED_STR, message)
    }
}