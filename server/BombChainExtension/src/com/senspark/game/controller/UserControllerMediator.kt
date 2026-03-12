package com.senspark.game.controller

import com.senspark.common.utils.IServerLogger
import com.senspark.game.declare.EnumConstants.DataType
import com.senspark.game.declare.EnumConstants.DeviceType
import com.senspark.game.declare.EnumConstants.Platform
import com.senspark.game.declare.EnumConstants.SAVE
import com.senspark.game.declare.EnumConstants.UserType
import com.senspark.game.extension.GlobalServices
import com.senspark.game.extension.ServerServices
import com.smartfoxserver.v2.entities.data.ISFSObject
import java.time.Instant

/**
 * Lưu ý: Class này chỉ dùng để sử dụng cho các Dependencies do UserController tạo ra
 * Các class Dependencies thay vì giữ trực tiếp reference của UserController, sẽ giữ object này
 */
data class UserControllerMediator(
    // fields:
    val userId: Int,
    val dataType: DataType,
    val userName: String,
    val userType: UserType,
    val deviceType: DeviceType,
    val platform: Platform?,
    val services: GlobalServices,
    val svServices: ServerServices,
    val logger: IServerLogger,

    // properties:
    var lastLogOut: () -> Instant?,

    // function
    val saveLater: (kind: SAVE) -> Unit,
    val saveImmediately: (kind: SAVE) -> Unit,
    val tryToKickAndWriteLogHack: (type: Int, data: String) -> Boolean,
    val setUsedPvpBoosterToDatabase: (itemId: Int) -> Unit,
    val saveGameAndLoadReward: () -> Unit,
    val isCheatByMultipleLogin: () -> Boolean,
    val sendDataEncryption: (cmd: String, data: ISFSObject, log: Boolean) -> Unit,
) {
    fun isAirdropUser(): Boolean {
        return dataType == DataType.TON ||
                dataType == DataType.SOL ||
                dataType == DataType.RON ||
                dataType == DataType.BAS ||
                dataType == DataType.VIC
    }
}
